import { cookies } from "next/headers";
import { NextResponse } from "next/server";
import { z } from "zod";

import {
  ACCESS_COOKIE,
  API_URL,
  REFRESH_COOKIE,
  clearSessionCookies,
  issueStepUp,
  isSameOriginMutation,
  readUser,
  setSessionCookies,
  toUser,
  type GatewayAuthResponse
} from "@/lib/server/session";

export const dynamic = "force-dynamic";

const loginSchema = z.object({
  email: z.string().email().max(255),
  password: z.string().min(1).max(128)
});

const registerSchema = z.object({
  fullName: z.string().min(2).max(200),
  email: z.string().email().max(255),
  password: z.string().min(12).max(128)
});

const stepUpSchema = z.object({ password: z.string().min(1).max(128) });

async function gateway(path: string, body: unknown, accessToken?: string) {
  return fetch(`${API_URL}${path}`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      ...(accessToken ? { Authorization: `Bearer ${accessToken}` } : {})
    },
    body: JSON.stringify(body),
    cache: "no-store"
  });
}

async function passthroughError(response: Response) {
  const data = (await response.json().catch(() => ({}))) as { message?: string };
  return NextResponse.json(
    { message: data.message ?? "Request failed" },
    { status: response.status === 401 ? 401 : response.status }
  );
}

export async function POST(request: Request, context: { params: Promise<{ action: string }> }) {
  const params = await context.params;
  if (!isSameOriginMutation(request)) {
    return NextResponse.json({ message: "Forbidden" }, { status: 403 });
  }
  const json = await request.json().catch(() => null);

  switch (params.action) {
    case "login": {
      const parsed = loginSchema.safeParse(json);
      if (!parsed.success) {
        return NextResponse.json({ message: "Enter your email and password" }, { status: 400 });
      }
      const upstream = await gateway("/api/v1/auth/login", parsed.data);
      if (!upstream.ok) {
        return passthroughError(upstream);
      }
      const auth = (await upstream.json()) as GatewayAuthResponse;
      const response = NextResponse.json({ user: toUser(auth) });
      setSessionCookies(response, auth);
      return response;
    }
    case "register": {
      const parsed = registerSchema.safeParse(json);
      if (!parsed.success) {
        return NextResponse.json({ message: "Check the highlighted fields" }, { status: 400 });
      }
      const [firstName, ...rest] = parsed.data.fullName.trim().split(/\s+/);
      const upstream = await gateway("/api/v1/auth/register", {
        email: parsed.data.email,
        password: parsed.data.password,
        firstName,
        lastName: rest.join(" ") || firstName
      });
      if (!upstream.ok) {
        return passthroughError(upstream);
      }
      const auth = (await upstream.json()) as GatewayAuthResponse;
      const fullName = parsed.data.fullName.trim();
      const response = NextResponse.json({ user: toUser(auth, fullName) });
      setSessionCookies(response, auth, fullName);
      return response;
    }
    case "logout": {
      const store = await cookies();
      const access = store.get(ACCESS_COOKIE)?.value;
      const refresh = store.get(REFRESH_COOKIE)?.value;
      if (access && refresh) {
        // Revoke both tokens server-side; clearing cookies alone would leave them valid.
        await gateway("/api/v1/auth/logout", { refreshToken: refresh }, access).catch(() => null);
      }
      const response = NextResponse.json({ ok: true });
      clearSessionCookies(response);
      return response;
    }
    case "step-up": {
      const user = await readUser();
      const parsed = stepUpSchema.safeParse(json);
      if (!user || !parsed.success) {
        return NextResponse.json({ message: "Enter your password" }, { status: 400 });
      }
      const upstream = await gateway("/api/v1/auth/login", {
        email: user.email,
        password: parsed.data.password
      });
      if (!upstream.ok) {
        return NextResponse.json({ message: "Password is incorrect" }, { status: 401 });
      }
      const auth = (await upstream.json()) as GatewayAuthResponse;
      if (String(auth.userId) !== user.id) {
        return NextResponse.json({ message: "Forbidden" }, { status: 403 });
      }
      const response = NextResponse.json({ ok: true });
      setSessionCookies(response, auth, user.fullName);
      issueStepUp(response, user.id);
      return response;
    }
    default:
      return NextResponse.json({ message: "Not found" }, { status: 404 });
  }
}
