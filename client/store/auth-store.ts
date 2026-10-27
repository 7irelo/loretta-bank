"use client";

import Cookies from "js-cookie";
import { create } from "zustand";

import type { SessionUser } from "@/types/api";

const USER_COOKIE = "lb_user";

/** Display-only session state. Tokens are HttpOnly and never reach this store. */
interface AuthStore {
  user: SessionUser | null;
  hydrated: boolean;
  setUser: (user: SessionUser | null) => void;
  hydrate: () => void;
}

const readUserCookie = (): SessionUser | null => {
  const raw = Cookies.get(USER_COOKIE);
  if (!raw) {
    return null;
  }
  try {
    return JSON.parse(decodeURIComponent(raw)) as SessionUser;
  } catch {
    return null;
  }
};

export const useAuthStore = create<AuthStore>((set) => ({
  user: null,
  hydrated: false,
  setUser: (user) => set({ user, hydrated: true }),
  hydrate: () => set({ user: readUserCookie(), hydrated: true })
}));
