"""LLM gateway: the only module that talks to model providers.

Routing, timeouts, retries, a circuit breaker, optional Bedrock fallback, refusal handling, cost
accounting and per-user budgets live here so features never import a provider SDK directly.
"""

from __future__ import annotations

import time
from dataclasses import dataclass, field
from typing import Any, TypeVar

import anthropic
from pydantic import BaseModel

from loretta_ai.config import Settings
from loretta_ai.telemetry import LLM_COST, LLM_LATENCY, LLM_REQUESTS, LLM_TOKENS, log, tracer

T = TypeVar("T", bound=BaseModel)

# Cheapest model that holds quality for each job; see docs/AI_PLAN.md section 1.
HAIKU = "claude-haiku-5-5"
SONNET = "claude-sonnet-5-5"
OPUS = "claude-opus-5-5"

# USD per million tokens: (input, output, cache read). List prices; update when they change.
PRICES: dict[str, tuple[float, float, float]] = {
    HAIKU: (0.10, 0.50, 0.01),
    SONNET: (2.00, 10.00, 0.20),
    OPUS: (4.00, 20.00, 0.20),
}


class LLMUnavailable(Exception):
    """No provider could answer; callers degrade gracefully."""


class LLMRefused(Exception):
    """The model declined; callers show a safe message."""


@dataclass
class CircuitBreaker:
    failure_threshold: int = 5
    reset_after_seconds: float = 30.0
    failures: int = 0
    opened_at: float | None = None

    def allow(self) -> bool:
        if self.opened_at is None:
            return True
        if time.monotonic() - self.opened_at >= self.reset_after_seconds:
            self.opened_at = None
            self.failures = 0
            return True
        return False

    def record(self, ok: bool) -> None:
        if ok:
            self.failures = 0
            return
        self.failures += 1
        if self.failures >= self.failure_threshold:
            self.opened_at = time.monotonic()


@dataclass
class Usage:
    model: str
    input_tokens: int = 0
    output_tokens: int = 0
    cache_read_tokens: int = 0
    cache_write_tokens: int = 0

    @property
    def cost_usd(self) -> float:
        price_in, price_out, price_cache = PRICES.get(self.model, (5.0, 25.0, 0.5))
        return (
            self.input_tokens * price_in
            + self.cache_write_tokens * price_in * 1.25
            + self.cache_read_tokens * price_cache
            + self.output_tokens * price_out
        ) / 1_000_000


@dataclass
class LLMGateway:
    settings: Settings
    primary: Any = None
    fallback: Any = None
    breakers: dict[str, CircuitBreaker] = field(default_factory=dict)

    @classmethod
    def from_settings(cls, settings: Settings) -> LLMGateway:
        primary = None
        if settings.llm_enabled and settings.anthropic_api_key is not None:
            primary = anthropic.AsyncAnthropic(
                api_key=settings.anthropic_api_key.get_secret_value(),
                timeout=settings.request_timeout_seconds,
                max_retries=2,
            )
        fallback = None
        if settings.bedrock_region:
            # Prefer the Messages-API ("Mantle") Bedrock client when the installed SDK has it.
            names = ("AsyncAnthropicBedrockMantle", "AsyncAnthropicBedrock")
            bedrock_cls = next(getattr(anthropic, n) for n in names if hasattr(anthropic, n))
            fallback = bedrock_cls(aws_region=settings.bedrock_region)
        return cls(settings=settings, primary=primary, fallback=fallback)

    @property
    def available(self) -> bool:
        return self.primary is not None

    async def parse(
        self,
        *,
        feature: str,
        model: str,
        system: str,
        messages: list[dict[str, Any]],
        output_format: type[T],
        max_tokens: int = 2048,
        effort: str = "low",
    ) -> tuple[T, Usage]:
        response, usage = await self._call(
            "parse",
            feature=feature,
            model=model,
            max_tokens=max_tokens,
            system=self._cached_system(system),
            messages=messages,
            output_format=output_format,
            output_config={"effort": effort},
        )
        parsed = getattr(response, "parsed_output", None)
        if response.stop_reason == "max_tokens":
            # Thinking counts against max_tokens; a truncated answer has no structured output.
            raise LLMUnavailable(f"{feature}: output truncated at max_tokens={max_tokens}")
        if parsed is None:
            raise LLMUnavailable("Model returned no structured output")
        return parsed, usage

    async def create(
        self,
        *,
        feature: str,
        model: str,
        system: str,
        messages: list[dict[str, Any]],
        tools: list[dict[str, Any]] | None = None,
        max_tokens: int = 2048,
        effort: str = "low",
    ) -> tuple[Any, Usage]:
        kwargs: dict[str, Any] = {
            "model": model,
            "max_tokens": max_tokens,
            "system": self._cached_system(system),
            "messages": messages,
            "output_config": {"effort": effort},
        }
        if tools:
            kwargs["tools"] = tools
        return await self._call("create", feature=feature, **kwargs)

    @staticmethod
    def _cached_system(system: str) -> list[dict[str, Any]]:
        # Stable instructions go first and are cached; volatile data goes in messages.
        return [{"type": "text", "text": system, "cache_control": {"type": "ephemeral"}}]

    async def _call(self, method: str, *, feature: str, **kwargs: Any) -> tuple[Any, Usage]:
        model: str = kwargs["model"]
        last_error: Exception | None = None
        for name, client in (("anthropic", self.primary), ("bedrock", self.fallback)):
            if client is None:
                continue
            breaker = self.breakers.setdefault(name, CircuitBreaker())
            if not breaker.allow():
                continue
            call_kwargs = dict(kwargs)
            if name == "bedrock":
                call_kwargs["model"] = f"anthropic.{model}"
            started = time.perf_counter()
            with tracer.start_as_current_span(f"chat {model}") as span:
                span.set_attribute("gen_ai.system", "anthropic")
                span.set_attribute("gen_ai.request.model", model)
                span.set_attribute("loretta.feature", feature)
                span.set_attribute("loretta.provider", name)
                try:
                    response = await getattr(client.messages, method)(**call_kwargs)
                except (
                    anthropic.APIConnectionError,
                    anthropic.RateLimitError,
                    anthropic.InternalServerError,
                ) as exc:
                    breaker.record(False)
                    LLM_REQUESTS.labels(feature, model, "error").inc()
                    log.warning("LLM %s via %s failed: %s", feature, name, type(exc).__name__)
                    last_error = exc
                    continue
                except anthropic.APIStatusError as exc:
                    LLM_REQUESTS.labels(feature, model, "rejected").inc()
                    raise LLMUnavailable(f"Model request rejected ({exc.status_code})") from exc
                breaker.record(True)
                elapsed = time.perf_counter() - started
                usage = self._usage(model, response)
                span.set_attribute("gen_ai.usage.input_tokens", usage.input_tokens)
                span.set_attribute("gen_ai.usage.output_tokens", usage.output_tokens)
                span.set_attribute("gen_ai.response.finish_reasons", [str(response.stop_reason)])
                LLM_LATENCY.labels(feature, model).observe(elapsed)
                LLM_COST.labels(feature, model).inc(usage.cost_usd)
                for kind, value in (
                    ("input", usage.input_tokens),
                    ("output", usage.output_tokens),
                    ("cache_read", usage.cache_read_tokens),
                    ("cache_write", usage.cache_write_tokens),
                ):
                    LLM_TOKENS.labels(feature, model, kind).inc(value)
                if response.stop_reason == "refusal":
                    LLM_REQUESTS.labels(feature, model, "refusal").inc()
                    raise LLMRefused("The model declined this request")
                LLM_REQUESTS.labels(feature, model, "ok").inc()
                return response, usage
        raise LLMUnavailable("No model provider is available") from last_error

    @staticmethod
    def _usage(model: str, response: Any) -> Usage:
        u = getattr(response, "usage", None)
        return Usage(
            model=model,
            input_tokens=getattr(u, "input_tokens", 0) or 0,
            output_tokens=getattr(u, "output_tokens", 0) or 0,
            cache_read_tokens=getattr(u, "cache_read_input_tokens", 0) or 0,
            cache_write_tokens=getattr(u, "cache_creation_input_tokens", 0) or 0,
        )
