"""FastAPI entry point. Reached only through api-gateway at /api/v1/ai/**."""

from __future__ import annotations

import base64
from contextlib import asynccontextmanager
from datetime import UTC, date, datetime
from decimal import Decimal
from typing import Annotated

import httpx
from fastapi import Depends, FastAPI, File, HTTPException, Request, UploadFile, status
from fastapi.responses import JSONResponse, Response
from prometheus_client import CONTENT_TYPE_LATEST, generate_latest
from pydantic import BaseModel, Field

from loretta_ai.bank import BankClient, BankError
from loretta_ai.budget import Budget
from loretta_ai.config import Settings, get_settings
from loretta_ai.features import assistant, categorize, help, insights, search, voice
from loretta_ai.llm import LLMGateway, LLMRefused, LLMUnavailable
from loretta_ai.security import Caller, customer_only
from loretta_ai.telemetry import log, setup_tracing

MAX_BODY_BYTES = 6 * 1024 * 1024


class State:
    llm: LLMGateway
    http: httpx.AsyncClient
    budget: Budget
    proposals: assistant.ProposalStore


state = State()


@asynccontextmanager
async def lifespan(_: FastAPI):
    settings = get_settings()
    setup_tracing(settings.otlp_endpoint)
    state.llm = LLMGateway.from_settings(settings)
    state.http = httpx.AsyncClient()
    state.budget = Budget(settings.daily_user_budget_cents, settings.redis_url)
    state.proposals = assistant.ProposalStore()
    log.info("ai-service ready (llm=%s)", state.llm.available)
    yield
    await state.http.aclose()


app = FastAPI(
    title="Loretta Bank AI",
    lifespan=lifespan,
    docs_url=None,
    redoc_url=None,
    openapi_url=None,
)


@app.middleware("http")
async def limits_and_headers(request: Request, call_next):
    length = request.headers.get("content-length")
    if length and int(length) > MAX_BODY_BYTES:
        return JSONResponse({"message": "Request too large"}, status_code=413)
    response = await call_next(request)
    response.headers["Cache-Control"] = "no-store"
    response.headers["X-Content-Type-Options"] = "nosniff"
    return response


@app.exception_handler(LLMUnavailable)
async def _unavailable(_: Request, __: LLMUnavailable) -> JSONResponse:
    return JSONResponse(
        {"message": "The assistant is temporarily unavailable. Everything else still works."},
        status_code=503,
    )


@app.exception_handler(LLMRefused)
async def _refused(_: Request, __: LLMRefused) -> JSONResponse:
    return JSONResponse({"message": "I can't help with that request."}, status_code=422)


@app.exception_handler(BankError)
async def _bank(_: Request, __: BankError) -> JSONResponse:
    return JSONResponse({"message": "Banking data is temporarily unavailable."}, status_code=503)


def require_llm() -> LLMGateway:
    if not state.llm.available:
        raise HTTPException(status.HTTP_503_SERVICE_UNAVAILABLE, "AI features are not configured")
    return state.llm


def bank_for(
    caller: Annotated[Caller, Depends(customer_only)],
    settings: Annotated[Settings, Depends(get_settings)],
) -> BankClient:
    return BankClient(settings, caller, state.http)


@app.get("/health")
async def health() -> dict[str, object]:
    return {"status": "ok", "llm": state.llm.available}


@app.get("/metrics")
async def metrics(request: Request) -> Response:
    # Scraped from inside the cluster only; the gateway does not route /metrics.
    return Response(generate_latest(), media_type=CONTENT_TYPE_LATEST)


class CategorizeRequest(BaseModel):
    description: str = Field(max_length=280)
    amount: Decimal = Field(gt=0, le=Decimal("10000000"))
    direction: str = Field(pattern="^(DEBIT|CREDIT)$")


@app.post("/api/v1/ai/categorize")
async def categorize_one(
    body: CategorizeRequest, caller: Annotated[Caller, Depends(customer_only)]
) -> categorize.Categorization:
    await state.budget.check(caller.user_id)
    result, cost = await categorize.categorize(
        state.llm, body.description, str(body.amount), body.direction
    )
    await state.budget.charge(caller.user_id, cost)
    return result


class QuestionRequest(BaseModel):
    question: str = Field(min_length=2, max_length=500)


@app.post("/api/v1/ai/search")
async def nl_search(
    body: QuestionRequest,
    caller: Annotated[Caller, Depends(customer_only)],
    bank: Annotated[BankClient, Depends(bank_for)],
    llm: Annotated[LLMGateway, Depends(require_llm)],
) -> search.SearchResult:
    await state.budget.check(caller.user_id)
    result, cost = await search.search(llm, bank, body.question)
    await state.budget.charge(caller.user_id, cost)
    return result


@app.post("/api/v1/ai/help")
async def help_answer(
    body: QuestionRequest,
    caller: Annotated[Caller, Depends(customer_only)],
    llm: Annotated[LLMGateway, Depends(require_llm)],
) -> help.HelpAnswer:
    await state.budget.check(caller.user_id)
    result, cost = await help.answer(llm, body.question)
    await state.budget.charge(caller.user_id, cost)
    return result


class InsightsResponse(BaseModel):
    month: str
    facts: insights.Facts
    insights: list[str]


@app.get("/api/v1/ai/insights")
async def monthly_insights(
    caller: Annotated[Caller, Depends(customer_only)],
    bank: Annotated[BankClient, Depends(bank_for)],
    llm: Annotated[LLMGateway, Depends(require_llm)],
) -> InsightsResponse:
    await state.budget.check(caller.user_id)
    today = datetime.now(UTC).date()
    month = date(today.year, today.month, 1)
    start_year, start_month = (
        (month.year, month.month - 3) if month.month > 3 else (month.year - 1, month.month + 9)
    )
    accounts = {a.id for a in await bank.accounts()}
    rows = await bank.transactions(
        start=datetime(start_year, start_month, 1, tzinfo=UTC), limit=200
    )
    outgoing = [
        (t.createdAt.date(), t.description or t.type, t.amount)
        for t in rows
        if t.status == "COMPLETED"
        and t.sourceAccountId in accounts
        and t.targetAccountId not in accounts
    ]
    facts = insights.build_facts(month, outgoing)
    texts: list[str] = []
    cost = 0.0
    if outgoing:
        texts, cost = await insights.write_insights(llm, facts)
    await state.budget.charge(caller.user_id, cost)
    return InsightsResponse(month=facts.month, facts=facts, insights=texts)


class ChatRequest(BaseModel):
    message: str = Field(min_length=1, max_length=2000)
    history: list[assistant.ChatTurn] = Field(default_factory=list, max_length=20)


@app.post("/api/v1/ai/assistant")
async def chat(
    body: ChatRequest,
    caller: Annotated[Caller, Depends(customer_only)],
    bank: Annotated[BankClient, Depends(bank_for)],
    llm: Annotated[LLMGateway, Depends(require_llm)],
) -> assistant.AssistantReply:
    await state.budget.check(caller.user_id)
    reply = await assistant.chat(
        llm, bank, state.proposals, caller.user_id, body.history, body.message
    )
    await state.budget.charge(caller.user_id, reply.cost_usd)
    return reply


@app.get("/api/v1/ai/proposals/{proposal_id}")
async def get_proposal(
    proposal_id: str, caller: Annotated[Caller, Depends(customer_only)]
) -> assistant.PaymentProposal:
    proposal = state.proposals.get(caller.user_id, proposal_id)
    if proposal is None:
        raise HTTPException(status.HTTP_404_NOT_FOUND, "Proposal not found or expired")
    return proposal


class VoiceReply(BaseModel):
    transcript: str
    reply: assistant.AssistantReply
    audio_base64: str | None


@app.post("/api/v1/ai/voice")
async def voice_turn(
    caller: Annotated[Caller, Depends(customer_only)],
    bank: Annotated[BankClient, Depends(bank_for)],
    llm: Annotated[LLMGateway, Depends(require_llm)],
    settings: Annotated[Settings, Depends(get_settings)],
    audio: UploadFile = File(...),
) -> VoiceReply:
    if settings.deepgram_api_key is None:
        raise HTTPException(status.HTTP_503_SERVICE_UNAVAILABLE, "Voice is not configured")
    key = settings.deepgram_api_key.get_secret_value()
    await state.budget.check(caller.user_id)
    data = await audio.read(voice.MAX_AUDIO_BYTES + 1)
    try:
        transcript = await voice.transcribe(
            state.http, key, data, audio.content_type or "application/octet-stream"
        )
    except voice.VoiceError as exc:
        raise HTTPException(status.HTTP_422_UNPROCESSABLE_ENTITY, str(exc)) from exc
    if not transcript:
        raise HTTPException(status.HTTP_422_UNPROCESSABLE_ENTITY, "I didn't catch that")
    reply = await assistant.chat(llm, bank, state.proposals, caller.user_id, [], transcript)
    await state.budget.charge(caller.user_id, reply.cost_usd)
    try:
        audio_out = base64.b64encode(await voice.speak(state.http, key, reply.text)).decode()
    except voice.VoiceError:
        audio_out = None
    return VoiceReply(transcript=transcript, reply=reply, audio_base64=audio_out)
