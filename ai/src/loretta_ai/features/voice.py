"""F8: voice banking through Deepgram (speech-to-text and text-to-speech).

Audio is transcribed with Deepgram, answered by the same assistant as chat (same tools, same
permissions), and the reply is synthesized back with Deepgram. Audio is never stored.
"""

from __future__ import annotations

import os

import httpx

DEEPGRAM_URL = "https://api.deepgram.com/v1"
STT_MODEL = os.environ.get("DEEPGRAM_STT_MODEL", "nova-3")
TTS_MODEL = os.environ.get("DEEPGRAM_TTS_MODEL", "aura-2-thalia-en")
MAX_AUDIO_BYTES = 5 * 1024 * 1024
ALLOWED_AUDIO = {"audio/wav", "audio/x-wav", "audio/webm", "audio/ogg", "audio/mpeg", "audio/mp4"}


class VoiceError(Exception):
    pass


async def transcribe(http: httpx.AsyncClient, api_key: str, audio: bytes, content_type: str) -> str:
    if content_type.split(";")[0] not in ALLOWED_AUDIO:
        raise VoiceError("Unsupported audio format")
    if len(audio) > MAX_AUDIO_BYTES:
        raise VoiceError("Audio is too long")
    response = await http.post(
        f"{DEEPGRAM_URL}/listen",
        params={"model": STT_MODEL, "smart_format": "true", "punctuate": "true"},
        headers={"Authorization": f"Token {api_key}", "Content-Type": content_type},
        content=audio,
        timeout=20.0,
    )
    if response.status_code >= 400:
        raise VoiceError(f"Transcription failed ({response.status_code})")
    data = response.json()
    try:
        return data["results"]["channels"][0]["alternatives"][0]["transcript"].strip()
    except (KeyError, IndexError) as exc:
        raise VoiceError("Transcription returned no text") from exc


async def speak(http: httpx.AsyncClient, api_key: str, text: str) -> bytes:
    response = await http.post(
        f"{DEEPGRAM_URL}/speak",
        params={"model": TTS_MODEL, "encoding": "mp3"},
        headers={"Authorization": f"Token {api_key}", "Content-Type": "application/json"},
        json={"text": text[:1500]},
        timeout=20.0,
    )
    if response.status_code >= 400:
        raise VoiceError(f"Speech synthesis failed ({response.status_code})")
    return response.content
