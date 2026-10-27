"""F3: help-centre answers with citations.

The corpus is small, so the whole knowledge base goes into a cached prompt as cited documents
(the "long context" arm of the plan). Answers without a citation are replaced with a referral,
so the assistant never states policy it cannot point to.
"""

from __future__ import annotations

from functools import lru_cache
from importlib import resources
from typing import Any

from pydantic import BaseModel

from loretta_ai.guardrails import sanitize_output
from loretta_ai.llm import SONNET, LLMGateway

SYSTEM = """You answer questions about Loretta Bank's products and policies using only the
provided documents. Quote or paraphrase only what the documents say. If they do not answer the
question, say you don't know and suggest contacting support. Be brief: three sentences at most.
Never give financial, legal or tax advice."""

NO_ANSWER = "I couldn't find that in our help documents. Please contact support and we'll help you."


class Citation(BaseModel):
    document: str
    quote: str


class HelpAnswer(BaseModel):
    answer: str
    citations: list[Citation]
    grounded: bool


@lru_cache
def load_documents() -> list[tuple[str, str]]:
    kb = resources.files("loretta_ai").joinpath("kb")
    docs = []
    for entry in sorted(kb.iterdir(), key=lambda e: e.name):
        if entry.name.endswith(".md"):
            docs.append((entry.name.removesuffix(".md"), entry.read_text(encoding="utf-8")))
    return docs


def document_blocks() -> list[dict[str, Any]]:
    blocks: list[dict[str, Any]] = [
        {
            "type": "document",
            "source": {"type": "text", "media_type": "text/plain", "data": text},
            "title": title,
            "citations": {"enabled": True},
        }
        for title, text in load_documents()
    ]
    blocks[-1]["cache_control"] = {"type": "ephemeral"}
    return blocks


def to_answer(response: Any) -> HelpAnswer:
    parts: list[str] = []
    citations: list[Citation] = []
    for block in response.content:
        if getattr(block, "type", None) != "text":
            continue
        parts.append(block.text)
        for c in getattr(block, "citations", None) or []:
            citations.append(
                Citation(document=getattr(c, "document_title", "") or "", quote=c.cited_text)
            )
    text = sanitize_output("".join(parts).strip())
    if not citations:
        return HelpAnswer(answer=NO_ANSWER, citations=[], grounded=False)
    return HelpAnswer(answer=text, citations=citations, grounded=True)


async def answer(llm: LLMGateway, question: str) -> tuple[HelpAnswer, float]:
    response, usage = await llm.create(
        feature="help",
        model=SONNET,
        system=SYSTEM,
        messages=[
            {
                "role": "user",
                "content": [*document_blocks(), {"type": "text", "text": question[:500]}],
            }
        ],
        max_tokens=2048,
        effort="low",
    )
    return to_answer(response), usage.cost_usd
