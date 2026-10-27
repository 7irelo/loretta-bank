import time

import jwt
import pytest
from fastapi import HTTPException

from loretta_ai.security import AUDIENCE, Caller, mint_internal_token, verify_internal_token

SECRET = "test-internal-secret-that-is-long-enough-0123456789"


def test_round_trip():
    token = mint_internal_token(Caller("7", frozenset({"CUSTOMER"})), SECRET)
    caller = verify_internal_token(token, SECRET)
    assert caller.user_id == "7" and caller.is_customer and not caller.is_staff


@pytest.mark.parametrize(
    "claims,secret",
    [
        (
            {"sub": "1", "aud": [AUDIENCE], "roles": ["ADMIN"]},
            "another-secret-long-enough-000000000000",
        ),
        ({"sub": "1", "aud": ["somewhere-else"], "roles": ["ADMIN"]}, SECRET),
        ({"sub": "1", "aud": [AUDIENCE], "roles": "ADMIN"}, SECRET),
    ],
)
def test_rejects_bad_tokens(claims, secret):
    token = jwt.encode({**claims, "exp": int(time.time()) + 60}, secret, algorithm="HS512")
    with pytest.raises(HTTPException):
        verify_internal_token(token, SECRET)


def test_rejects_expired_tokens():
    token = jwt.encode(
        {"sub": "1", "aud": [AUDIENCE], "roles": ["CUSTOMER"], "exp": int(time.time()) - 5},
        SECRET,
        algorithm="HS512",
    )
    with pytest.raises(HTTPException):
        verify_internal_token(token, SECRET)


def test_rejects_unsigned_tokens():
    token = jwt.encode(
        {"sub": "1", "aud": [AUDIENCE], "roles": ["ADMIN"], "exp": int(time.time()) + 60},
        key=None,
        algorithm="none",
    )
    with pytest.raises(HTTPException):
        verify_internal_token(token, SECRET)
