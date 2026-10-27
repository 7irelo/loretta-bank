import { cookies } from "next/headers";
import { NextResponse } from "next/server";

import {
  ACCESS_COOKIE,
  API_URL,
  REFRESH_COOKIE,
  clearSessionCookies,
  hasValidStepUp,
  isSameOriginMutation,
  readUser,
  refreshTokens,
  setSessionCookies
} from "@/lib/server/session";

export const dynamic = "force-dynamic";

const ALLOWED_PREFIXES = [
  "api/v1/customers",
  "api/v1/accounts",
  "api/v1/transactions",
  "api/v1/transfers",
  "api/v1/notifications",
  "api/v1/reports",
  "api/v1/audit",
  "api/v1/auth/users",
  "api/v1/ai/"
];
const MAX_BODY_BYTES = 6 * 1024 * 1024;

function allowed(path: string): boolean {
  if (path.includes("..") || path.includes("//")) {
    return false;
  }
  return ALLOWED_PREFIXES.some(
    (prefix) => path === prefix.replace(/\/$/, "") || path.startsWith(prefix.endsWith("/") ? prefix : `${prefix}/`)
  );
}

/** Paying an account that isn't yours requires a recent password re-entry (step-up). */
function needsStepUp(method: string, path: string, body: ArrayBuffer | undefined): boolean {
  if (method !== "POST" || path !== "api/v1/transfers" || !body) {
    return false;
  }
  try {
    const payload = JSON.parse(new TextDecoder().decode(body)) as { targetAccountNumber?: string };
    return Boolean(payload.targetAccountNumber);
  } catch {
    return true;
  }
}

async function forward(request: Request, path: string, accessToken: string, body?: ArrayBuffer) {
  const url = new URL(request.url);
  const headers: Record<string, string> = { Authorization: `Bearer ${accessToken}` };
  for (const name of ["content-type", "idempotency-key", "accept"]) {
    const value = request.headers.get(name);
    if (value) {
      headers[name] = value;
    }
  }
  return fetch(`${API_URL}/${path}${url.search}`, {
    method: request.method,
    headers,
    body: body && body.byteLength > 0 ? body : undefined,
    cache: "no-store",
    redirect: "manual"
  });
}

async function handle(request: Request, { params }: { params: { path: string[] } }) {
  const path = params.path.join("/");
  if (!allowed(path)) {
    return NextResponse.json({ message: "Not found" }, { status: 404 });
  }
  if (!isSameOriginMutation(request)) {
    return NextResponse.json({ message: "Forbidden" }, { status: 403 });
  }
  const length = Number(request.headers.get("content-length") ?? "0");
  if (length > MAX_BODY_BYTES) {
    return NextResponse.json({ message: "Request too large" }, { status: 413 });
  }

  const body = ["GET", "HEAD"].includes(request.method) ? undefined : await request.arrayBuffer();
  if (needsStepUp(request.method, path, body) && !hasValidStepUp(readUser()?.id)) {
    return NextResponse.json(
      { message: "Confirm your password to pay a new account", code: "STEP_UP_REQUIRED" },
      { status: 428 }
    );
  }

  const store = cookies();
  let accessToken = store.get(ACCESS_COOKIE)?.value;
  let refreshed: Awaited<ReturnType<typeof refreshTokens>> = null;

  if (!accessToken) {
    const refreshToken = store.get(REFRESH_COOKIE)?.value;
    refreshed = refreshToken ? await refreshTokens(refreshToken) : null;
    accessToken = refreshed?.accessToken;
  }
  if (!accessToken) {
    const response = NextResponse.json({ message: "Session expired" }, { status: 401 });
    clearSessionCookies(response);
    return response;
  }

  let upstream = await forward(request, path, accessToken, body);
  if (upstream.status === 401 && !refreshed) {
    const refreshToken = store.get(REFRESH_COOKIE)?.value;
    refreshed = refreshToken ? await refreshTokens(refreshToken) : null;
    if (refreshed) {
      upstream = await forward(request, path, refreshed.accessToken, body);
    }
  }

  const response = new NextResponse(upstream.body, {
    status: upstream.status,
    headers: {
      "content-type": upstream.headers.get("content-type") ?? "application/json",
      "cache-control": "no-store"
    }
  });
  if (refreshed) {
    setSessionCookies(response, refreshed, readUser()?.fullName);
  }
  if (upstream.status === 401) {
    clearSessionCookies(response);
  }
  return response;
}

export { handle as DELETE, handle as GET, handle as PATCH, handle as POST, handle as PUT };
