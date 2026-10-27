# ai-service

Python service behind `api-gateway` at `/api/v1/ai/**`. It provides categorization,
natural-language transaction search, cited help answers, monthly insights, the banking assistant,
and voice (Deepgram).

```bash
uv sync
uv run uvicorn loretta_ai.main:app --port 8090     # needs INTERNAL_JWT_SECRET
uv run pytest                                      # offline: fake model and bank
uv run pyright && uv run ruff check .
uv run python -m evals.run --trials 3              # live models: needs ANTHROPIC_API_KEY
```

## Layout

| Path | Purpose |
|------|---------|
| `src/loretta_ai/llm.py` | The only module that calls model providers: routing, retries, circuit breaker, Bedrock fallback, cost metrics, refusal handling |
| `src/loretta_ai/security.py` | Verifies the gateway's internal token; mints a user-scoped token for every downstream call |
| `src/loretta_ai/guardrails.py` | PII redaction, untrusted-data wrapping, injection screening, output sanitizing |
| `src/loretta_ai/bank.py` | User-scoped client for the banking services (read-only) |
| `src/loretta_ai/features/` | One module per feature |
| `src/loretta_ai/kb/` | Help-centre documents used for cited answers |
| `evals/` | Golden sets, runner, thresholds; reports go to `evals/reports/` |

## Configuration

| Variable | Required | Purpose |
|----------|----------|---------|
| `INTERNAL_JWT_SECRET` | yes | Verifies gateway tokens and signs downstream tokens |
| `ANTHROPIC_API_KEY` | for AI features | Claude models; without it the AI endpoints return 503 |
| `DEEPGRAM_API_KEY` | for voice | Speech-to-text and text-to-speech |
| `BEDROCK_REGION` | no | Enables fallback to Claude on Amazon Bedrock |
| `REDIS_URL` | no | Shared per-user spend caps (in-memory otherwise) |
| `AI_DAILY_USER_BUDGET_CENTS` | no | Per-user daily model spend cap (default 50) |
| `CUSTOMER_SERVICE_URL`, `ACCOUNT_SERVICE_URL`, `TRANSACTION_SERVICE_URL` | yes | Banking services |
