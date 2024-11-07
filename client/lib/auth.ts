import Cookies from "js-cookie";

import type { AuthSession, AuthUser, UserRole } from "@/types/auth";

export const TOKEN_COOKIE_NAME = "lb_access_token";
export const USER_COOKIE_NAME = "lb_user";

const COOKIE_OPTIONS = {
  expires: 1,
  secure: process.env.NODE_ENV === "production",
  sameSite: "strict" as const
};

const asString = (value: unknown): string => (typeof value === "string" ? value.trim() : "");

const normalizeRole = (rawRole: string): UserRole => {
  const value = rawRole.toUpperCase();

  if (value === "ADMIN" || value === "SUPPORT") {
    return value;
  }

  return "CUSTOMER";
};

export const normalizeAuthSession = (payload: unknown): AuthSession => {
  const source = (payload ?? {}) as Record<string, unknown>;
  const token =
    asString(source.token) ||
    asString(source.accessToken) ||
    asString(source.jwt) ||
    asString(source.idToken);

  if (!token) {
    throw new Error("Authentication token missing in response.");
  }

  const userSource = ((source.user ?? source.profile ?? source.account ?? source) ?? {}) as Record<
    string,
    unknown
  >;

  const fallbackName = asString(userSource.name) || asString(userSource.username) || "Loretta User";
  const fullName =
    asString(userSource.fullName) ||
    [asString(userSource.firstName), asString(userSource.lastName)].filter(Boolean).join(" ") ||
    fallbackName;

  const email = asString(userSource.email) || asString(source.email);

  const user: AuthUser = {
    id: asString(userSource.id) || asString(source.userId) || "unknown-user",
    fullName,
    email,
    role: normalizeRole(asString(userSource.role) || asString(source.role) || "CUSTOMER")
  };

  return { token, user };
};

export const setAuthCookies = (session: AuthSession): void => {
  if (typeof window === "undefined") {
    return;
  }

  Cookies.set(TOKEN_COOKIE_NAME, session.token, COOKIE_OPTIONS);
  Cookies.set(USER_COOKIE_NAME, encodeURIComponent(JSON.stringify(session.user)), COOKIE_OPTIONS);
};

export const clearAuthCookies = (): void => {
  if (typeof window === "undefined") {
    return;
  }

  Cookies.remove(TOKEN_COOKIE_NAME);
  Cookies.remove(USER_COOKIE_NAME);
};

export const getAccessToken = (): string | null => {
  if (typeof window === "undefined") {
    return null;
  }

  return Cookies.get(TOKEN_COOKIE_NAME) ?? null;
};

export const getAuthUserFromCookie = (): AuthUser | null => {
  if (typeof window === "undefined") {
    return null;
  }

  const raw = Cookies.get(USER_COOKIE_NAME);

  if (!raw) {
    return null;
  }

  try {
    return JSON.parse(decodeURIComponent(raw)) as AuthUser;
  } catch {
    return null;
  }
};

export const isRoleAllowed = (role: UserRole | null | undefined, allowedRoles: UserRole[]): boolean => {
  if (!role) {
    return false;
  }

  return allowedRoles.includes(role);
};

