"""User-scoped client for the core banking services.

Every request carries a token minted for the *calling user*, so the Java services apply the same
ownership and role checks they apply to the web app. The AI service holds no elevated access.
"""

from __future__ import annotations

from datetime import datetime
from decimal import Decimal
from typing import Any

import httpx
from pydantic import BaseModel

from loretta_ai.config import Settings
from loretta_ai.security import Caller, mint_internal_token


class Account(BaseModel):
    id: int
    accountNumber: str
    accountType: str
    currency: str
    balance: Decimal
    status: str


class Txn(BaseModel):
    id: str
    type: str
    status: str
    sourceAccountId: int | None = None
    targetAccountId: int | None = None
    amount: Decimal
    currency: str
    description: str | None = None
    reference: str | None = None
    createdAt: datetime


class BankError(Exception):
    pass


class BankClient:
    def __init__(self, settings: Settings, caller: Caller, http: httpx.AsyncClient) -> None:
        self._settings = settings
        self._caller = caller
        self._http = http

    def _headers(self) -> dict[str, str]:
        token = mint_internal_token(
            self._caller, self._settings.internal_jwt_secret.get_secret_value()
        )
        return {"X-Internal-Token": token}

    async def _get(self, url: str, params: dict[str, Any] | None = None) -> Any:
        try:
            response = await self._http.get(
                url, params=params, headers=self._headers(), timeout=5.0
            )
        except httpx.HTTPError as exc:
            raise BankError("Banking service unavailable") from exc
        if response.status_code >= 400:
            raise BankError(f"Banking service returned {response.status_code}")
        return response.json()

    async def customer_id(self) -> int:
        data = await self._get(f"{self._settings.customer_service_url}/api/v1/customers/me")
        return int(data["id"])

    async def accounts(self) -> list[Account]:
        customer_id = await self.customer_id()
        data = await self._get(
            f"{self._settings.account_service_url}/api/v1/accounts/customer/{customer_id}"
        )
        return [Account.model_validate(a) for a in data]

    async def transactions(
        self,
        *,
        type_: str | None = None,
        start: datetime | None = None,
        end: datetime | None = None,
        text: str | None = None,
        limit: int = 200,
    ) -> list[Txn]:
        params: dict[str, Any] = {"page": 0, "size": min(max(limit, 1), 200)}
        if type_:
            params["type"] = type_
        if start:
            params["from"] = start.isoformat()
        if end:
            params["to"] = end.isoformat()
        if text:
            params["q"] = text[:100]
        data = await self._get(
            f"{self._settings.transaction_service_url}/api/v1/transactions/mine", params
        )
        return [Txn.model_validate(t) for t in data.get("content", [])]
