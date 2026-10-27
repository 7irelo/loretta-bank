"""Metrics and traces for every model call, following the OpenTelemetry GenAI conventions."""

from __future__ import annotations

import logging

from opentelemetry import trace
from opentelemetry.sdk.resources import Resource
from opentelemetry.sdk.trace import TracerProvider
from opentelemetry.sdk.trace.export import BatchSpanProcessor
from prometheus_client import Counter, Histogram

log = logging.getLogger("loretta_ai")

LLM_REQUESTS = Counter("llm_requests_total", "Model calls", ["feature", "model", "outcome"])
LLM_TOKENS = Counter("llm_tokens_total", "Tokens by kind", ["feature", "model", "kind"])
LLM_COST = Counter("llm_cost_usd_total", "Estimated spend in USD", ["feature", "model"])
LLM_LATENCY = Histogram(
    "llm_latency_seconds",
    "Model call latency",
    ["feature", "model"],
    buckets=(0.1, 0.25, 0.5, 1, 2, 4, 8, 16, 32),
)
GUARDRAIL_HITS = Counter("guardrail_hits_total", "Guardrail activations", ["guardrail"])

tracer = trace.get_tracer("loretta_ai")


def setup_tracing(otlp_endpoint: str | None) -> None:
    provider = TracerProvider(resource=Resource.create({"service.name": "ai-service"}))
    if otlp_endpoint:
        from opentelemetry.exporter.otlp.proto.http.trace_exporter import OTLPSpanExporter

        provider.add_span_processor(
            BatchSpanProcessor(OTLPSpanExporter(endpoint=f"{otlp_endpoint}/v1/traces"))
        )
    trace.set_tracer_provider(provider)
