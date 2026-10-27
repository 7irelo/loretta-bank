import type { NextRequest } from "next/server";
import { NextResponse } from "next/server";

const AUTH_ROUTES = new Set(["/login", "/register"]);
const STAFF_PREFIX = "/admin";

const roleFrom = (request: NextRequest): string | null => {
  const raw = request.cookies.get("lb_user")?.value;
  if (!raw) {
    return null;
  }
  try {
    return (JSON.parse(decodeURIComponent(raw)) as { role?: string }).role ?? null;
  } catch {
    return null;
  }
};

/**
 * Routing guard only. Real authorization happens in the gateway and services; this just keeps
 * signed-out users off app pages and customers off staff pages.
 */
export function middleware(request: NextRequest) {
  const { pathname } = request.nextUrl;
  const signedIn = Boolean(
    request.cookies.get("lb_at")?.value || request.cookies.get("lb_user")?.value
  );

  if (AUTH_ROUTES.has(pathname)) {
    return signedIn ? NextResponse.redirect(new URL("/dashboard", request.url)) : NextResponse.next();
  }
  if (pathname === "/") {
    return NextResponse.redirect(new URL(signedIn ? "/dashboard" : "/login", request.url));
  }
  if (!signedIn) {
    const login = new URL("/login", request.url);
    login.searchParams.set("from", pathname);
    return NextResponse.redirect(login);
  }
  const role = roleFrom(request);
  if (pathname.startsWith(STAFF_PREFIX) && role !== "ADMIN" && role !== "SUPPORT") {
    return NextResponse.redirect(new URL("/dashboard", request.url));
  }
  return NextResponse.next();
}

export const config = {
  matcher: [
    "/",
    "/login",
    "/register",
    "/onboarding/:path*",
    "/dashboard/:path*",
    "/accounts/:path*",
    "/transfer/:path*",
    "/transactions/:path*",
    "/statements/:path*",
    "/notifications/:path*",
    "/assistant/:path*",
    "/settings/:path*",
    "/admin/:path*"
  ]
};
