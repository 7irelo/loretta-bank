"""End-to-end journey against a running stack (docker compose up).

Drives signup, KYC, deposits, transfers, limits, access control, reconciliation, statements,
notifications, the AI endpoints and the web app's session protections, then exits non-zero on
any failure. Never prints secrets.

    set -a; . ./.env; set +a
    GW=http://127.0.0.1:8080 WEB=http://127.0.0.1:3000 python scripts/e2e_journey.py
"""
import os, sys, time, uuid
from decimal import Decimal
import httpx

GW = os.environ.get("GW", "http://127.0.0.1:8080")
WEB = os.environ.get("WEB", "http://127.0.0.1:3000")
ADMIN_PASSWORD = os.environ["ADMIN_PASSWORD"]
results = []

def check(name, ok, detail=""):
    results.append((name, bool(ok)))
    print(("PASS " if ok else "FAIL ") + name + (f"  [{detail}]" if detail and not ok else ""))

c = httpx.Client(base_url=GW, timeout=60)
run = uuid.uuid4().hex[:6]

def register(first):
    email = f"{first.lower()}.{run}@example.co.za"
    r = c.post("/api/v1/auth/register", json={"email": email, "password": "Str0ng!Passw0rd", "firstName": first, "lastName": "Test"})
    assert r.status_code == 201, r.text
    return r.json()

def H(tok, key=None):
    h = {"Authorization": f"Bearer {tok}"}
    if key:
        h["Idempotency-Key"] = key
    return h

# --- auth policy -------------------------------------------------------------------------
r = c.post("/api/v1/auth/register", json={"email": f"weak.{run}@example.co.za", "password": "short", "firstName": "W", "lastName": "W"})
check("weak password rejected", r.status_code == 400, r.status_code)

a = register("Ayanda"); b = register("Bongani")
ta, tb = a["accessToken"], b["accessToken"]
admin = c.post("/api/v1/auth/login", json={"email": "admin@lorettabank.co.za", "password": ADMIN_PASSWORD})
check("admin can sign in with generated password", admin.status_code == 200, admin.status_code)
tadm = admin.json()["accessToken"]

ids = {"Ayanda": "9501015800087", "Bongani": "8001015009087"}
cust = {}
for user, tok, name in [(a, ta, "Ayanda"), (b, tb, "Bongani")]:
    r = c.post("/api/v1/customers", headers=H(tok), json={
        "userId": user["userId"], "firstName": name, "lastName": "Test", "email": user["email"],
        "phoneNumber": "0821234567", "idNumber": ids[name], "dateOfBirth": "1995-01-01",
        "address": "1 Test Street, Johannesburg"})
    check(f"{name} customer profile created", r.status_code == 201, r.text[:200])
    cust[name] = r.json()["id"]

r = c.post("/api/v1/customers", headers=H(ta), json={"userId": a["userId"], "firstName": "X", "lastName": "Y",
    "email": f"x{run}@example.co.za", "phoneNumber": "0821234567", "idNumber": "9501015800089", "dateOfBirth": "1995-01-01", "address": "1 Test Street, Johannesburg"})
check("invalid SA ID rejected", r.status_code in (400, 409), r.status_code)

r = c.post("/api/v1/accounts", headers=H(ta), json={"customerId": cust["Ayanda"], "accountType": "CHECKING"})
check("unverified customer cannot open account", r.status_code == 403, r.status_code)

for name in cust:
    r = c.patch(f"/api/v1/customers/{cust[name]}/kyc", params={"status": "VERIFIED"}, headers=H(tadm))
    check(f"admin verifies {name}", r.status_code == 200, r.status_code)

r = c.patch(f"/api/v1/customers/{cust['Bongani']}/kyc", params={"status": "VERIFIED"}, headers=H(ta))
check("customer cannot verify KYC", r.status_code == 403, r.status_code)

def open_account(tok, cid, kind):
    r = c.post("/api/v1/accounts", headers=H(tok), json={"customerId": cid, "accountType": kind})
    assert r.status_code == 201, r.text
    return r.json()

a_chk = open_account(ta, cust["Ayanda"], "CHECKING")
a_sav = open_account(ta, cust["Ayanda"], "SAVINGS")
b_chk = open_account(tb, cust["Bongani"], "CHECKING")
check("verified customers open accounts", True)

# --- money ------------------------------------------------------------------------------
r = c.post("/api/v1/transactions/deposit", headers=H(ta, str(uuid.uuid4())), json={"accountId": a_chk["id"], "amount": 1000000, "currency": "ZAR"})
check("customer cannot deposit (create money)", r.status_code == 403, r.status_code)

r = c.post("/api/v1/transactions/deposit", headers=H(tadm, f"dep-{run}"), json={"accountId": a_chk["id"], "amount": 1000.00, "currency": "ZAR", "description": "Branch cash"})
check("admin cash deposit", r.status_code in (201, 202) and r.json()["status"] == "COMPLETED", r.text[:200])

k_own = f"own-{run}"
r1 = c.post("/api/v1/transfers", headers=H(ta, k_own), json={"sourceAccountId": a_chk["id"], "targetAccountId": a_sav["id"], "amount": 200.00, "currency": "ZAR", "description": "Savings"})
r2 = c.post("/api/v1/transfers", headers=H(ta, k_own), json={"sourceAccountId": a_chk["id"], "targetAccountId": a_sav["id"], "amount": 200.00, "currency": "ZAR", "description": "Savings"})
check("own transfer completes", r1.status_code == 201 and r1.json()["status"] == "COMPLETED", r1.text[:200])
check("idempotent replay returns same transfer", r2.json().get("transferId") == r1.json().get("transferId"))

r = c.post("/api/v1/transfers", headers=H(ta, f"pay-{run}"), json={"sourceAccountId": a_chk["id"], "targetAccountNumber": b_chk["accountNumber"], "amount": 150.00, "currency": "ZAR", "description": "Lunch"})
check("pay another customer by account number", r.status_code == 201 and r.json()["status"] == "COMPLETED", r.text[:200])
pay_id = r.json().get("transferId")

r = c.post("/api/v1/transfers", headers=H(ta, f"big-{run}"), json={"sourceAccountId": a_chk["id"], "targetAccountId": a_sav["id"], "amount": 60000, "currency": "ZAR"})
check("per-payment limit enforced", r.status_code in (400, 422), r.status_code)

r = c.post("/api/v1/transfers", headers=H(ta, f"over-{run}"), json={"sourceAccountId": a_chk["id"], "targetAccountId": a_sav["id"], "amount": 5000, "currency": "ZAR"})
check("insufficient funds rejected", r.status_code in (400, 422) and "nsufficient" in r.text, r.text[:200])

r = c.post("/api/v1/transfers", headers=H(tb, f"steal-{run}"), json={"sourceAccountId": a_chk["id"], "targetAccountId": b_chk["id"], "amount": 10, "currency": "ZAR"})
check("cannot pay from someone else's account", r.status_code == 403, r.status_code)

bal = lambda tok, acc: Decimal(str(c.get(f"/api/v1/accounts/{acc['id']}", headers=H(tok)).json()["balance"]))
check("balances: A cheque 650", bal(ta, a_chk) == Decimal("650"), bal(ta, a_chk))
check("balances: A savings 200", bal(ta, a_sav) == Decimal("200"))
check("balances: B cheque 150", bal(tb, b_chk) == Decimal("150"))

# --- access control ---------------------------------------------------------------------
r = c.get(f"/api/v1/accounts/{a_chk['id']}", headers=H(tb))
check("customer cannot read another's account", r.status_code == 403, r.status_code)
r = c.get(f"/api/v1/accounts/{a_chk['id']}", headers={**H(tb), "X-User-Id": "1", "X-User-Roles": "ADMIN"})
check("spoofed identity headers ignored", r.status_code == 403, r.status_code)
r = c.get(f"/api/v1/transactions/account/{a_chk['id']}", headers=H(tb))
check("cannot read another's transactions", r.status_code == 403, r.status_code)
r = c.get("/api/v1/auth/users", headers=H(ta))
check("customer cannot list users", r.status_code == 403, r.status_code)
r = c.get("/internal/v1/accounts/1", headers=H(tadm))
check("internal API not reachable through gateway", r.status_code == 404, r.status_code)
r = c.get(f"/api/v1/accounts/{a_chk['id']}", headers={"Authorization": f"Bearer {a['refreshToken']}"})
check("refresh token cannot be used as access token", r.status_code == 401, r.status_code)

# --- ledger, statements, notifications -------------------------------------------------
r = c.get("/api/v1/accounts/admin/reconciliation", headers=H(tadm))
rep = r.json()
check("ledger reconciles (sum 0, no drift)", r.status_code == 200 and rep["healthy"], rep)

today = time.strftime("%Y-%m-%d")
r = c.post("/api/v1/reports/statements", headers=H(ta), json={"accountId": a_chk["id"], "periodFrom": "2026-01-01T00:00:00", "periodTo": f"{today}T23:59:59"})
st = r.json() if r.status_code == 201 else {}
ok = st and Decimal(str(st["openingBalance"])) + Decimal(str(st["totalCredits"])) - Decimal(str(st["totalDebits"])) == Decimal(str(st["closingBalance"])) and Decimal(str(st["closingBalance"])) == Decimal("650")
check("statement reconciles: opening + in - out = closing (650)", ok, r.text[:300])

received = False
for _ in range(20):
    r = c.get("/api/v1/notifications", headers=H(tb))
    if r.status_code == 200 and any("received" in n["subject"].lower() for n in r.json()["content"]):
        received = True
        break
    time.sleep(3)
check("recipient gets 'payment received' notification", received)

r = c.post("/api/v1/auth/logout", headers=H(tb), json={"refreshToken": b["refreshToken"]})
r2 = c.get("/api/v1/notifications", headers=H(tb))
check("logout revokes access token", r.status_code == 204 and r2.status_code == 401, (r.status_code, r2.status_code))

# --- AI -----------------------------------------------------------------------------------
r = c.post("/api/v1/ai/assistant", headers=H(ta), json={"message": "What is the balance of my cheque account?", "history": []})
check("assistant answers with real balance", r.status_code == 200 and "650" in r.json()["text"], r.text[:300])
r = c.post("/api/v1/ai/search", headers=H(ta), json={"question": "How much did I transfer this month?"})
check("NL search works", r.status_code == 200 and r.json()["count"] >= 1, r.text[:300])
r = c.post("/api/v1/ai/help", headers=H(ta), json={"question": "What is the daily payment limit?"})
check("help answer is cited", r.status_code == 200 and r.json()["grounded"], r.text[:300])
r = c.post("/api/v1/ai/assistant", headers=H(tadm), json={"message": "hi", "history": []})
check("AI customer features refuse staff tokens", r.status_code == 403, r.status_code)

# --- web BFF ---------------------------------------------------------------------------
w = httpx.Client(base_url=WEB, timeout=60)
origin = {"Origin": WEB, "x-loretta-csrf": "1"}
r = w.post("/api/auth/login", json={"email": a["email"], "password": "Str0ng!Passw0rd"})
check("BFF rejects login without CSRF header/origin", r.status_code == 403, r.status_code)
r = w.post("/api/auth/login", json={"email": a["email"], "password": "Str0ng!Passw0rd"}, headers=origin)
sc = r.headers.get_list("set-cookie")
check("BFF login sets HttpOnly session cookies", r.status_code == 200 and any(x.startswith("lb_at=") and "HttpOnly" in x for x in sc) and "accessToken" not in r.text, r.status_code)
# Session cookies are Secure; browsers send them to localhost over http, httpx does not.
w.headers["Cookie"] = "; ".join(x.split(";")[0] for x in sc)
r = w.get(f"/api/proxy/api/v1/accounts/customer/{cust['Ayanda']}")
check("BFF proxy returns data with cookie session", r.status_code == 200 and len(r.json()) == 2, r.status_code)
r = w.post("/api/proxy/api/v1/transfers", json={"sourceAccountId": a_chk["id"], "targetAccountId": a_sav["id"], "amount": 1}, headers={**origin, "Idempotency-Key": str(uuid.uuid4())})
check("BFF own-account transfer works", r.status_code in (201, 202), r.text[:200])
r = w.post("/api/proxy/api/v1/transfers", json={"sourceAccountId": a_chk["id"], "targetAccountNumber": b_chk["accountNumber"], "amount": 1}, headers={**origin, "Idempotency-Key": str(uuid.uuid4())})
check("paying a new account requires step-up", r.status_code == 428, r.status_code)
r = w.post("/api/proxy/api/v1/transfers", json={}, headers={"Origin": "https://evil.example", "x-loretta-csrf": "1"})
check("cross-site mutation blocked", r.status_code == 403, r.status_code)
r = w.get("/api/proxy/internal/v1/accounts/1")
check("BFF proxy blocks non-allowlisted paths", r.status_code == 404, r.status_code)
r = w.get("/login")
csp = r.headers.get("content-security-policy", "")
check("CSP and frame protections set", "frame-ancestors 'none'" in csp and r.headers.get("x-frame-options") == "DENY")

failed = [n for n, ok in results if not ok]
print(f"\n{len(results) - len(failed)}/{len(results)} checks passed")
sys.exit(1 if failed else 0)
