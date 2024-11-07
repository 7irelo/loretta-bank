"use client";

import { create } from "zustand";

import { clearAuthCookies, getAccessToken, getAuthUserFromCookie, setAuthCookies } from "@/lib/auth";
import type { AuthSession, AuthUser } from "@/types/auth";

interface AuthStore {
  token: string | null;
  user: AuthUser | null;
  hydrated: boolean;
  setSession: (session: AuthSession) => void;
  clearSession: () => void;
  hydrateFromCookies: () => void;
}

export const useAuthStore = create<AuthStore>((set) => ({
  token: null,
  user: null,
  hydrated: false,
  setSession: (session) => {
    setAuthCookies(session);
    set({ token: session.token, user: session.user, hydrated: true });
  },
  clearSession: () => {
    clearAuthCookies();
    set({ token: null, user: null, hydrated: true });
  },
  hydrateFromCookies: () => {
    const token = getAccessToken();
    const user = getAuthUserFromCookie();

    set({ token, user, hydrated: true });
  }
}));

