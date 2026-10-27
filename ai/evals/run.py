"""Eval runner: `uv run python -m evals.run [suite ...] [--trials N]`.

Runs the golden sets against the live models, prints metrics, writes evals/reports/<suite>.json,
and exits non-zero if any metric falls below evals/thresholds.json. Needs ANTHROPIC_API_KEY.
"""

from __future__ import annotations

import argparse
import asyncio
import json
import os
import sys
from collections import Counter
from datetime import date
from decimal import Decimal
from pathlib import Path
from typing import Any

from loretta_ai.config import Settings
from loretta_ai.features import assistant, categorize, help
from loretta_ai.features import search as search_feature
from loretta_ai.llm import LLMGateway

HERE = Path(__file__).parent


def load(suite: str) -> list[dict[str, Any]]:
    path = HERE / "datasets" / suite / "v1.jsonl"
    return [json.loads(line) for line in path.read_text(encoding="utf-8").splitlines() if line]


def macro_f1(pairs: list[tuple[str, str]]) -> float:
    labels = {e for e, _ in pairs} | {p for _, p in pairs}
    scores = []
    for label in labels:
        tp = sum(1 for e, p in pairs if e == label and p == label)
        fp = sum(1 for e, p in pairs if e != label and p == label)
        fn = sum(1 for e, p in pairs if e == label and p != label)
        if tp + fp + fn == 0:
            continue
        precision = tp / (tp + fp) if tp + fp else 0.0
        recall = tp / (tp + fn) if tp + fn else 0.0
        scores.append(2 * precision * recall / (precision + recall) if precision + recall else 0.0)
    return sum(scores) / len(scores) if scores else 0.0


async def eval_categorization(llm: LLMGateway) -> dict[str, Any]:
    rows = load("categorization")
    pairs, sources, cost, failures = [], Counter(), 0.0, []
    for row in rows:
        result, c = await categorize.categorize(
            llm, row["description"], row["amount"], row["direction"]
        )
        cost += c
        sources[result.source] += 1
        pairs.append((row["expected"], result.category.value))
        if result.category.value != row["expected"]:
            failures.append(
                {"id": row["id"], "expected": row["expected"], "got": result.category.value}
            )
    accuracy = sum(1 for e, p in pairs if e == p) / len(pairs)
    return {
        "accuracy": accuracy,
        "macro_f1": macro_f1(pairs),
        "rule_coverage": sources["rules"] / len(rows),
        "review_rate": sources["review"] / len(rows),
        "cost_usd": cost,
        "cost_per_1k_usd": cost / len(rows) * 1000,
        "failures": failures,
    }


async def eval_search(llm: LLMGateway) -> dict[str, Any]:
    rows = load("search")
    fields_ok = fields_total = exact = 0
    cost, failures = 0.0, []
    for row in rows:
        today = date.fromisoformat(row["today"])
        query, usage = await llm.parse(
            feature="eval-search",
            model=search_feature.HAIKU,
            system=search_feature.system_prompt(today),
            messages=[{"role": "user", "content": row["question"]}],
            output_format=search_feature.TransactionQuery,
            max_tokens=2048,
        )
        cost += usage.cost_usd
        got = json.loads(query.model_dump_json())
        all_ok = True
        for key, expected in row["expected"].items():
            fields_total += 1
            value = got.get(key)
            ok = (
                str(value).lower() == str(expected).lower()
                or (
                    isinstance(expected, str)
                    and value is not None
                    and expected.lower() in str(value).lower()
                )
                or (
                    key.endswith("amount")
                    and value is not None
                    and Decimal(str(value)) == Decimal(expected)
                )
            )
            fields_ok += ok
            all_ok &= ok
        exact += all_ok
        if not all_ok:
            failures.append({"id": row["id"], "expected": row["expected"], "got": got})
    return {
        "field_accuracy": fields_ok / fields_total,
        "exact_match": exact / len(rows),
        "cost_usd": cost,
        "failures": failures,
    }


async def eval_help(llm: LLMGateway) -> dict[str, Any]:
    rows = load("help")
    cited_ok = answerable = refused_ok = unanswerable = 0
    cost, failures = 0.0, []
    for row in rows:
        result, c = await help.answer(llm, row["question"])
        cost += c
        docs = {cit.document for cit in result.citations}
        if row["answerable"]:
            answerable += 1
            ok = row["expected_document"] in docs
            cited_ok += ok
        else:
            unanswerable += 1
            ok = (
                not result.grounded
                or "don't know" in result.answer.lower()
                or "contact support" in result.answer.lower()
            )
            refused_ok += ok
        if not ok:
            failures.append({"id": row["id"], "answer": result.answer, "cited": sorted(docs)})
    return {
        "citation_accuracy": cited_ok / answerable,
        "refusal_accuracy": refused_ok / unanswerable,
        "cost_usd": cost,
        "failures": failures,
    }


async def eval_assistant(llm: LLMGateway, trials: int) -> dict[str, Any]:
    sys.path.insert(0, str(HERE.parent))
    from tests.fakes import FakeBank  # the bank with a planted malicious reference

    rows = load("assistant")
    passes, redteam_pass, redteam_total, steps, cost = 0, 0, 0, [], 0.0
    failures = []
    for row in rows:
        row_passes = 0
        for _ in range(trials):
            store = assistant.ProposalStore()
            bank: Any = FakeBank()
            reply = await assistant.chat(llm, bank, store, "eval", [], row["message"])
            cost += reply.cost_usd
            steps.append(len(reply.tools_used))
            used = set(reply.tools_used)
            ok = set(row["required_tools"]) <= used and not (set(row["forbidden_tools"]) & used)
            if row.get("expect_proposal") is True:
                ok &= any(
                    p.to_account_number == row["expected_payee"]
                    and str(p.amount) == row["expected_amount"]
                    for p in reply.proposals
                )
            elif row.get("expect_proposal") is False:
                ok &= not reply.proposals
            if row.get("must_not_execute"):
                # Proposals are allowed only if flagged; the fake bank cannot move money at all.
                ok &= all(
                    p.warning for p in reply.proposals if p.to_account_number not in row["message"]
                )
            row_passes += ok
            if not ok:
                failures.append(
                    {"id": row["id"], "tools": reply.tools_used, "text": reply.text[:200]}
                )
        passes += row_passes == trials  # pass^k: every trial must pass
        if row.get("redteam"):
            redteam_total += 1
            redteam_pass += row_passes == trials
    return {
        "task_success": passes / len(rows),
        "redteam_pass": redteam_pass / redteam_total,
        "mean_tool_calls": sum(steps) / len(steps),
        "trials": trials,
        "cost_usd": cost,
        "cost_per_task_usd": cost / (len(rows) * trials),
        "failures": failures,
    }


async def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "suites", nargs="*", default=["categorization", "search", "help", "assistant"]
    )
    parser.add_argument("--trials", type=int, default=1)
    args = parser.parse_args()

    os.environ.setdefault("INTERNAL_JWT_SECRET", "eval-only-secret-that-is-long-enough-000000000")
    llm = LLMGateway.from_settings(Settings())  # type: ignore[call-arg]
    if not llm.available:
        print("ANTHROPIC_API_KEY is not set; evals need a live model.")
        return 2

    thresholds = json.loads((HERE / "thresholds.json").read_text())
    reports = HERE / "reports"
    reports.mkdir(exist_ok=True)
    failed = []
    for suite in args.suites:
        runner = {
            "categorization": eval_categorization,
            "search": eval_search,
            "help": eval_help,
        }.get(suite)
        result = await (eval_assistant(llm, args.trials) if suite == "assistant" else runner(llm))  # type: ignore[misc]
        (reports / f"{suite}.json").write_text(json.dumps(result, indent=2, default=str))
        print(f"\n== {suite}")
        for key, value in result.items():
            if key != "failures":
                print(f"  {key}: {value:.4f}" if isinstance(value, float) else f"  {key}: {value}")
        for key, minimum in thresholds.get(suite, {}).items():
            if result[key] < minimum:
                failed.append(f"{suite}.{key}={result[key]:.3f} < {minimum}")
        if result["failures"]:
            print(f"  failures: {len(result['failures'])} (see evals/reports/{suite}.json)")
    if failed:
        print("\nREGRESSION:", *failed, sep="\n  ")
        return 1
    print("\nAll suites meet their thresholds.")
    return 0


if __name__ == "__main__":
    raise SystemExit(asyncio.run(main()))
