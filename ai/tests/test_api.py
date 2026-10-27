import time

import jwt
from fastapi.testclient import TestClient

from loretta_ai.main import app
from loretta_ai.security import AUDIENCE

SECRET = "test-internal-secret-that-is-long-enough-0123456789"


def token(roles):
    return jwt.encode(
        {"sub": "5", "aud": [AUDIENCE], "roles": roles, "exp": int(time.time()) + 60},
        SECRET,
        algorithm="HS512",
    )


def test_requires_internal_token():
    with TestClient(app) as client:
        assert client.post("/api/v1/ai/help", json={"question": "fees?"}).status_code == 401


def test_customer_features_reject_staff():
    with TestClient(app) as client:
        r = client.post(
            "/api/v1/ai/help",
            json={"question": "fees?"},
            headers={"X-Internal-Token": token(["ADMIN"])},
        )
        assert r.status_code == 403


def test_degrades_when_no_model_is_configured(monkeypatch):
    monkeypatch.delenv("ANTHROPIC_API_KEY", raising=False)
    with TestClient(app) as client:
        r = client.post(
            "/api/v1/ai/help",
            json={"question": "What are the fees?"},
            headers={"X-Internal-Token": token(["CUSTOMER"])},
        )
        assert r.status_code == 503


def test_rejects_oversized_bodies():
    with TestClient(app) as client:
        r = client.post(
            "/api/v1/ai/help",
            content=b"x" * 10,
            headers={
                "X-Internal-Token": token(["CUSTOMER"]),
                "content-length": str(50 * 1024 * 1024),
            },
        )
        assert r.status_code == 413
