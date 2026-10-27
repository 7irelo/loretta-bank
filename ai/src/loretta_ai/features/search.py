"""F2: natural-language transaction search.

The model only fills in a typed filter. The banking API applies it, and totals are computed here
in Decimal. The model never sees the results and never does arithmetic.
"""

from __future__ import annotations

from datetime import date, datetime, time, timedelta
from decimal import Decimal
from enum import StrEnum
from zoneinfo import ZoneInfo

from pydantic import BaseModel, Field, field_validator

from loretta_ai.bank import BankClient, Txn
from loretta_ai.llm import HAIKU, LLMGateway

BANK_ZONE = ZoneInfo("Africa/Johannesburg")


class TxnType(StrEnum):
    DEPOSIT = "DEPOSIT"
    WITHDRAWAL = "WITHDRAWAL"
    TRANSFER = "TRANSFER"


class Aggregation(StrEnum):
    LIST = "list"
    SUM = "sum"
    COUNT = "count"
    LARGEST = "largest"


class TransactionQuery(BaseModel):
    text: str | None = Field(default=None, max_length=60, description="Merchant or keyword, if any")
    type: TxnType | None = None
    start_date: date | None = None
    end_date: date | None = Field(default=None, description="Inclusive")
    min_amount: Decimal | None = Field(default=None, ge=0)
    max_amount: Decimal | None = Field(default=None, ge=0)
    aggregation: Aggregation = Aggregation.LIST

    @field_validator("end_date")
    @classmethod
    def _end_after_start(cls, v: date | None, info) -> date | None:  # type: ignore[no-untyped-def]
        start = info.data.get("start_date")
        if v and start and v < start:
            raise ValueError("end_date before start_date")
        return v


class SearchResult(BaseModel):
    query: TransactionQuery
    answer: str
    total: Decimal | None = None
    count: int
    transactions: list[Txn]


def system_prompt(today: date) -> str:
    return f"""Convert a customer's question about their own transactions into a filter.
Today is {today.isoformat()} (Africa/Johannesburg). Resolve relative dates ("last month",
"since Friday") to explicit dates. Use aggregation "sum" for "how much", "count" for
"how many", "largest" for "biggest/largest", otherwise "list". Leave fields empty when the
question does not constrain them. Never invent merchants."""


def _money(value: Decimal) -> str:
    return f"R{value.quantize(Decimal('0.01')):,}".replace(",", " ")


def apply_query(query: TransactionQuery, rows: list[Txn]) -> SearchResult:
    filtered = [
        t
        for t in rows
        if t.status == "COMPLETED"
        and (query.min_amount is None or t.amount >= query.min_amount)
        and (query.max_amount is None or t.amount <= query.max_amount)
    ]
    total: Decimal | None = None
    if query.aggregation == Aggregation.SUM:
        total = sum((t.amount for t in filtered), Decimal("0"))
        answer = f"{_money(total)} across {len(filtered)} transactions."
    elif query.aggregation == Aggregation.COUNT:
        answer = f"{len(filtered)} transactions."
    elif query.aggregation == Aggregation.LARGEST and filtered:
        top = max(filtered, key=lambda t: t.amount)
        filtered = [top]
        answer = f"Largest was {_money(top.amount)} on {top.createdAt.date().isoformat()}."
    else:
        answer = f"Found {len(filtered)} transactions."
    return SearchResult(
        query=query, answer=answer, total=total, count=len(filtered), transactions=filtered[:50]
    )


async def search(llm: LLMGateway, bank: BankClient, question: str) -> tuple[SearchResult, float]:
    today = datetime.now(BANK_ZONE).date()
    query, usage = await llm.parse(
        feature="search",
        model=HAIKU,
        system=system_prompt(today),
        messages=[{"role": "user", "content": question[:300]}],
        output_format=TransactionQuery,
        max_tokens=2048,
        effort="low",
    )
    start = datetime.combine(query.start_date, time.min, BANK_ZONE) if query.start_date else None
    end = (
        datetime.combine(query.end_date + timedelta(days=1), time.min, BANK_ZONE)
        if query.end_date
        else None
    )
    rows = await bank.transactions(
        type_=query.type.value if query.type else None, start=start, end=end, text=query.text
    )
    return apply_query(query, rows), usage.cost_usd
