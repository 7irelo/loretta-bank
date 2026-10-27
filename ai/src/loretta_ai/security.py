"""Caller identity.

The gateway authenticates the user and forwards a short-lived signed X-Internal-Token. We verify
it here and mint an equivalent token for downstream calls, so every tool the model uses runs with
exactly the user's own permissions and nothing more.
"""

from __future__ import annotations

import time
from dataclasses import dataclass

import jwt
from fastapi import Depends, Header, HTTPException, status

from loretta_ai.config import Settings, get_settings

AUDIENCE = "loretta-internal"
TOKEN_TTL_SECONDS = 60


@dataclass(frozen=True)
class Caller:
    user_id: str
    roles: frozenset[str]

    @property
    def is_customer(self) -> bool:
        return "CUSTOMER" in self.roles

    @property
    def is_staff(self) -> bool:
        return bool({"ADMIN", "SUPPORT"} & self.roles)


def verify_internal_token(token: str, secret: str) -> Caller:
    try:
        claims = jwt.decode(
            token,
            secret,
            algorithms=["HS256", "HS384", "HS512"],
            audience=AUDIENCE,
            options={"require": ["exp", "sub", "aud"]},
        )
    except jwt.PyJWTError as exc:
        raise HTTPException(status.HTTP_401_UNAUTHORIZED, "Invalid internal token") from exc
    roles = claims.get("roles") or []
    if not isinstance(roles, list) or not all(isinstance(r, str) for r in roles):
        raise HTTPException(status.HTTP_401_UNAUTHORIZED, "Invalid roles claim")
    return Caller(user_id=str(claims["sub"]), roles=frozenset(roles))


def mint_internal_token(caller: Caller, secret: str) -> str:
    now = int(time.time())
    return jwt.encode(
        {
            "sub": caller.user_id,
            "aud": [AUDIENCE],
            "roles": sorted(caller.roles),
            "iat": now,
            "exp": now + TOKEN_TTL_SECONDS,
        },
        secret,
        algorithm="HS512",
    )


def current_caller(
    x_internal_token: str | None = Header(default=None),
    settings: Settings = Depends(get_settings),
) -> Caller:
    if not x_internal_token:
        raise HTTPException(status.HTTP_401_UNAUTHORIZED, "Missing internal token")
    return verify_internal_token(x_internal_token, settings.internal_jwt_secret.get_secret_value())


def customer_only(caller: Caller = Depends(current_caller)) -> Caller:
    if not caller.is_customer:
        raise HTTPException(status.HTTP_403_FORBIDDEN, "Customer features only")
    return caller
