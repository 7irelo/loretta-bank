"""PII redaction and prompt-injection defences.

Transaction descriptions and payment references are written by whoever sends the money, so they
are attacker-controlled. Everything that did not come from the signed-in user or from us is
treated as data, wrapped, and screened before a model sees it.
"""

from __future__ import annotations

import html
import re
from dataclasses import dataclass

_SA_ID = re.compile(r"(?<!\d)\d{13}(?!\d)")
_CARD = re.compile(r"(?<!\d)(?:\d[ -]?){13,19}(?!\d)")
_ACCOUNT = re.compile(r"\bLOR\d{10}\b")
_EMAIL = re.compile(r"[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}")
_PHONE = re.compile(r"(?<!\d)(?:\+27|0)[1-9]\d{8}(?!\d)")


def _luhn_ok(digits: str) -> bool:
    total = 0
    for i, ch in enumerate(reversed(digits)):
        d = int(ch)
        if i % 2 == 1:
            d *= 2
            if d > 9:
                d -= 9
        total += d
    return total % 10 == 0


def redact(text: str) -> str:
    """Masks personal data before text is logged, traced or sent to a provider."""

    def card(match: re.Match[str]) -> str:
        digits = re.sub(r"\D", "", match.group(0))
        return "[CARD]" if 13 <= len(digits) <= 19 and _luhn_ok(digits) else match.group(0)

    text = _EMAIL.sub("[EMAIL]", text)
    text = _SA_ID.sub("[ID_NUMBER]", text)
    text = _CARD.sub(card, text)
    text = _ACCOUNT.sub(lambda m: "LOR******" + m.group(0)[-4:], text)
    text = _PHONE.sub("[PHONE]", text)
    return text


_INJECTION_PATTERNS = [
    r"ignore (all |any )?(previous|prior|above) (instructions|messages)",
    r"disregard (the )?(system|previous) (prompt|instructions)",
    r"you are now",
    r"system prompt",
    r"</?(system|assistant|instructions?|tool_result)>",
    r"\b(transfer|send|pay|draft_payment)\b.{0,40}\b(to|into)\b.{0,40}\b(LOR\d{10}|account)",
    r"act as",
    r"new instructions",
]
_INJECTION = re.compile("|".join(f"(?:{p})" for p in _INJECTION_PATTERNS), re.IGNORECASE)


@dataclass(frozen=True)
class Screened:
    text: str
    suspicious: bool


def screen_untrusted(text: str | None, max_len: int = 280) -> Screened:
    """Neutralises third-party text: truncate, redact PII, flag instruction-like content."""
    raw = (text or "")[:max_len]
    suspicious = bool(_INJECTION.search(raw))
    cleaned = redact(raw)
    if suspicious:
        cleaned = "[content withheld: looked like instructions]"
    return Screened(text=cleaned, suspicious=suspicious)


def wrap_untrusted(label: str, payload: str) -> str:
    """Marks a block as data. The system prompt tells the model never to follow it."""
    return (
        f'<untrusted_data source="{html.escape(label)}">\n{html.escape(payload)}\n</untrusted_data>'
    )


_SAFE_LINK = re.compile(r"https?://(?!(?:www\.)?lorettabank\.co\.za\b)\S+", re.IGNORECASE)
_MARKDOWN_IMAGE = re.compile(r"!\[[^\]]*\]\([^)]*\)")


def sanitize_output(text: str) -> str:
    """No images, and no links except our own domain; blocks data exfiltration via URLs."""
    text = _MARKDOWN_IMAGE.sub("", text)
    text = _SAFE_LINK.sub("[link removed]", text)
    return redact(text)
