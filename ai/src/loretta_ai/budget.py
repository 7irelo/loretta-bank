"""Per-user daily spend caps, so one account cannot run up the model bill."""

from __future__ import annotations

from datetime import UTC, datetime

from fastapi import HTTPException, status

try:
    import redis.asyncio as redis_async
except ImportError:  # pragma: no cover
    redis_async = None  # type: ignore[assignment]


class Budget:
    def __init__(self, daily_cents: int, redis_url: str | None) -> None:
        self._daily_micro = daily_cents * 10_000  # cents -> micro-dollars
        self._memory: dict[str, int] = {}
        self._redis = (
            redis_async.from_url(redis_url) if redis_url and redis_async is not None else None
        )

    @staticmethod
    def _key(user_id: str) -> str:
        return f"ai:spend:{datetime.now(UTC).date().isoformat()}:{user_id}"

    async def _spent(self, key: str) -> int:
        if self._redis is not None:
            value = await self._redis.get(key)
            return int(value or 0)
        return self._memory.get(key, 0)

    async def check(self, user_id: str) -> None:
        if await self._spent(self._key(user_id)) >= self._daily_micro:
            raise HTTPException(
                status.HTTP_429_TOO_MANY_REQUESTS,
                "You've reached today's assistant limit. It resets at midnight UTC.",
            )

    async def charge(self, user_id: str, cost_usd: float) -> None:
        micro = int(cost_usd * 1_000_000)
        key = self._key(user_id)
        if self._redis is not None:
            await self._redis.incrby(key, micro)
            await self._redis.expire(key, 2 * 86_400)
        else:
            self._memory[key] = self._memory.get(key, 0) + micro
            if len(self._memory) > 10_000:
                today = datetime.now(UTC).date().isoformat()
                self._memory = {k: v for k, v in self._memory.items() if today in k}
