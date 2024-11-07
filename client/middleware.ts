import type { NextRequest } from "next/server";
import { NextResponse } from "next/server";

const AUTH_ROUTES = new Set(["/login", "/register"]);
const PROTECTED_PREFIXES = ["/dashboard", "/accounts", "/transfer", "/transactions", "/admin"];

const parseRole = (request: NextRequest): string | null => {
  const rawUserCookie = request.cookies.get("lb_user")?.value;

  if (!rawUserCookie) {
    return null;
  }

  try {
    const parsed = JSON.parse(decodeURIComponent(rawUserCookie)) as { role?: string };

    return typeof parsed.role === "string" ? parsed.role.toUpperCase() : null;
  } catch {
    return null;
  }
};

export function middleware(request: NextRequest) {
  const { pathname } = request.nextUrl;
  const token = request.cookies.get("lb_access_token")?.value;
  const isProtectedRoute = PROTECTED_PREFIXES.some(
    (prefix) => pathname === prefix || pathname.startsWith(`${prefix}/`)
  );

  if (isProtectedRoute && !token) {
    const loginUrl = new URL("/login", request.url);
    loginUrl.searchParams.set("from", pathname);
    return NextResponse.redirect(loginUrl);
  }

  if (AUTH_ROUTES.has(pathname) && token) {
    return NextResponse.redirect(new URL("/dashboard", request.url));
  }

  if ((pathname === "/admin" || pathname.startsWith("/admin/")) && token) {
    const role = parseRole(request);

    if (role !== "ADMIN" && role !== "SUPPORT") {
      return NextResponse.redirect(new URL("/dashboard", request.url));
    }
  }

  return NextResponse.next();
}

export const config = {
  matcher: [
    "/",
    "/login",
    "/register",
    "/dashboard/:path*",
    "/accounts/:path*",
    "/transfer/:path*",
    "/transactions/:path*",
    "/admin/:path*"
  ]
};

