"""F7: monthly spending insights.

Facts (totals, deltas, top merchants) are computed here in Decimal. The model only writes short
sentences around them, and a deterministic groundedness check drops any insight that mentions a
number or merchant that is not in the facts.
"""

from __future__ import annotations

import re
from collections import defaultdict
from datetime import date
from decimal import Decimal

from pydantic import BaseModel, Field

from loretta_ai.features.categorize import categorize_by_rules
from loretta_ai.guardrails import sanitize_output
from loretta_ai.llm import SONNET, LLMGateway


class CategoryFact(BaseModel):
    category: str
    this_month: Decimal
    three_month_average: Decimal


class Facts(BaseModel):
    month: str
    total_spent: Decimal
    categories: list[CategoryFact]
    top_merchants: list[tuple[str, Decimal]]


class Insight(BaseModel):
    text: str = Field(max_length=200)


class InsightDraft(BaseModel):
    insights: list[Insight] = Field(max_length=5)


SYSTEM = """You write short, friendly spending insights for a bank customer.
Use only the numbers and merchant names in the facts. Do not calculate new numbers. Write amounts
exactly as given, prefixed with R. Write at most 5 insights, each one sentence. No advice about
investments, credit or tax."""

_NUMBER = re.compile(r"R?\s?(\d[\d\s]*(?:[.,]\d{1,2})?)")


def build_facts(month: date, rows: list[tuple[date, str, Decimal]]) -> Facts:
    """rows: (date, description, outgoing amount) for the month and the three months before it."""
    month_key = (month.year, month.month)
    by_category: dict[str, dict[tuple[int, int], Decimal]] = defaultdict(
        lambda: defaultdict(Decimal)
    )
    merchants: dict[str, Decimal] = defaultdict(Decimal)
    for day, description, amount in rows:
        ruled = categorize_by_rules(description)
        category = ruled.category.value if ruled else "other"
        by_category[category][(day.year, day.month)] += amount
        if (day.year, day.month) == month_key and ruled:
            merchants[ruled.merchant] += amount

    previous = []
    y, m = month_key
    for _ in range(3):
        y, m = (y - 1, 12) if m == 1 else (y, m - 1)
        previous.append((y, m))

    categories = []
    for category, months in by_category.items():
        current = months.get(month_key, Decimal("0"))
        average = sum((months.get(p, Decimal("0")) for p in previous), Decimal("0")) / 3
        categories.append(
            CategoryFact(
                category=category,
                this_month=current.quantize(Decimal("0.01")),
                three_month_average=average.quantize(Decimal("0.01")),
            )
        )
    categories.sort(key=lambda c: c.this_month, reverse=True)
    top = sorted(merchants.items(), key=lambda kv: kv[1], reverse=True)[:3]
    return Facts(
        month=f"{month_key[0]}-{month_key[1]:02d}",
        total_spent=sum((c.this_month for c in categories), Decimal("0")),
        categories=categories,
        top_merchants=[(name, value.quantize(Decimal("0.01"))) for name, value in top],
    )


def allowed_numbers(facts: Facts) -> set[Decimal]:
    values = {facts.total_spent}
    for c in facts.categories:
        values |= {c.this_month, c.three_month_average}
    values |= {v for _, v in facts.top_merchants}
    allowed = set()
    for v in values:
        allowed.add(v.quantize(Decimal("0.01")))
        allowed.add(v.quantize(Decimal("1")))
    return allowed


def is_grounded(text: str, facts: Facts) -> bool:
    allowed = allowed_numbers(facts)
    for match in _NUMBER.finditer(text):
        raw = match.group(1).replace(" ", "").replace(",", ".")
        try:
            value = Decimal(raw)
        except ArithmeticError:
            return False
        if value < 13 and not text[match.start() : match.start() + 1] == "R":
            continue  # small counts like "3 merchants" or "1 month" are not money
        if (
            value.quantize(Decimal("0.01")) not in allowed
            and value.quantize(Decimal("1")) not in allowed
        ):
            return False
    return True


async def write_insights(llm: LLMGateway, facts: Facts) -> tuple[list[str], float]:
    draft, usage = await llm.parse(
        feature="insights",
        model=SONNET,
        system=SYSTEM,
        messages=[{"role": "user", "content": facts.model_dump_json()}],
        output_format=InsightDraft,
        max_tokens=2048,
        effort="low",
    )
    kept = [sanitize_output(i.text) for i in draft.insights if is_grounded(i.text, facts)]
    return kept, usage.cost_usd
