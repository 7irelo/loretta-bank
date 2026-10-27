import "server-only";

import { createHmac, randomBytes, timingSafeEqual } from "node:crypto";
import { cookies } from "next/headers";
import type { NextResponse } from "next/server";

/**
 * Backend-for-frontend session. Access and refresh tokens live only in HttpOnly cookies, so
 * page JavaScript (and any injected script) can never read them. The browser talks to
 * same-origin route handlers, which attach the token when calling the gateway.
 */

export const API_URL = process.env.API_URL ?? "http://localhost:8080";

export const ACCESS_COOKIE = "lb_at";
export const REFRESH_COOKIE = "lb_rt";
export const USER_COOKIE = "lb_user";
export const STEP_UP_COOKIE = "lb_su";

const secure = process.env.NODE_ENV === "production";
const STEP_UP_SECONDS = 5 * 60;
// Signs step-up cookies. Set SESSION_SECRET in production; a per-process key is used otherwise.
const SESSION_SECRET = process.env.SESSION_SECRET ?? randomBytes(32).toString("hex");

export interface PublicUser {
  id: string;
  email: string;
  fullName: string;
  role: "ADMIN" | "CUSTOMER" | "SUPPORT";
}

interface GatewayAuthResponse {
  accessToken: string;
  refreshToken: string;
  userId: number;
  email: string;
  role: PublicUser["role"];
  firstName?: string;
  lastName?: string;
}

export function toUser(auth: GatewayAuthResponse, fullName?: string): PublicUser {
  return {
    id: String(auth.userId),
    email: auth.email,
    role: auth.role,
    fullName: fullName || [auth.firstName, auth.lastName].filter(Boolean).join(" ") || auth.email
  };
}

export function setSessionCookies(
  response: NextResponse,
  auth: GatewayAuthResponse,
  fullName?: string
): PublicUser {
  const user = toUser(auth, fullName);
  response.cookies.set(ACCESS_COOKIE, auth.accessToken, {
    httpOnly: true,
    secure,
    sameSite: "strict",
    path: "/",
    maxAge: 15 * 60
  });
  response.cookies.set(REFRESH_COOKIE, auth.refreshToken, {
    httpOnly: true,
    secure,
    sameSite: "strict",
    path: "/api",
    maxAge: 7 * 24 * 60 * 60
  });
  // Display-only profile; contains no secrets and is safe for page scripts to read.
  response.cookies.set(USER_COOKIE, encodeURIComponent(JSON.stringify(user)), {
    httpOnly: false,
    secure,
    sameSite: "strict",
    path: "/",
    maxAge: 7 * 24 * 60 * 60
  });
  return user;
}

export function clearSessionCookies(response: NextResponse): void {
  for (const [name, path] of [
    [ACCESS_COOKIE, "/"],
    [REFRESH_COOKIE, "/api"],
    [USER_COOKIE, "/"],
    [STEP_UP_COOKIE, "/"]
  ] as const) {
    response.cookies.set(name, "", { path, maxAge: 0 });
  }
}

export function readUser(): PublicUser | null {
  const raw = cookies().get(USER_COOKIE)?.value;
  if (!raw) {
    return null;
  }
  try {
    return JSON.parse(decodeURIComponent(raw)) as PublicUser;
  } catch {
    return null;
  }
}

function sign(value: string): string {
  return createHmac("sha256", SESSION_SECRET).update(value).digest("base64url");
}

export function issueStepUp(response: NextResponse, userId: string): void {
  const expires = Math.floor(Date.now() / 1000) + STEP_UP_SECONDS;
  const payload = `${userId}.${expires}`;
  response.cookies.set(STEP_UP_COOKIE, `${payload}.${sign(payload)}`, {
    httpOnly: true,
    secure,
    sameSite: "strict",
    path: "/",
    maxAge: STEP_UP_SECONDS
  });
}

export function hasValidStepUp(userId: string | undefined): boolean {
  const raw = cookies().get(STEP_UP_COOKIE)?.value;
  if (!raw || !userId) {
    return false;
  }
  const [id, expires, signature] = raw.split(".");
  if (!id || !expires || !signature || id !== userId) {
    return false;
  }
  if (Number(expires) < Math.floor(Date.now() / 1000)) {
    return false;
  }
  const expected = Buffer.from(sign(`${id}.${expires}`));
  const actual = Buffer.from(signature);
  return expected.length === actual.length && timingSafeEqual(expected, actual);
}

/** Rejects cross-site requests: same-origin Origin plus a custom header a form can't send. */
export function isSameOriginMutation(request: Request): boolean {
  if (["GET", "HEAD", "OPTIONS"].includes(request.method)) {
    return true;
  }
  if (request.headers.get("x-loretta-csrf") !== "1") {
    return false;
  }
  const origin = request.headers.get("origin");
  const host = request.headers.get("x-forwarded-host") ?? request.headers.get("host");
  if (!origin || !host) {
    return false;
  }
  try {
    return new URL(origin).host === host;
  } catch {
    return false;
  }
}

export async function refreshTokens(refreshToken: string): Promise<GatewayAuthResponse | null> {
  const response = await fetch(`${API_URL}/api/v1/auth/refresh`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ refreshToken }),
    cache: "no-store"
  });
  if (!response.ok) {
    return null;
  }
  return (await response.json()) as GatewayAuthResponse;
}

export type { GatewayAuthResponse };
