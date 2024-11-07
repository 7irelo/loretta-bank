"use client";

import axios, { AxiosError } from "axios";
import toast from "react-hot-toast";

import { clearAuthCookies, getAccessToken, normalizeAuthSession } from "@/lib/auth";
import { useAuthStore } from "@/store/auth-store";
import type { CreateAccountPayload, Account } from "@/types/account";
import type { PaginatedResponse } from "@/types/api";
import type { Customer } from "@/types/customer";
import type { AuthSession, LoginPayload, RegisterPayload } from "@/types/auth";
import type { Transaction, TransactionFilters, TransferPayload } from "@/types/transaction";

const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";

type ApiRecord = Record<string, unknown>;

const isRecord = (value: unknown): value is ApiRecord =>
  typeof value === "object" && value !== null && !Array.isArray(value);

const unwrapApiPayload = <T>(payload: unknown): T => {
  if (isRecord(payload) && "data" in payload) {
    return payload.data as T;
  }

  return payload as T;
};

export const normalizePaginatedResponse = <T>(payload: unknown): PaginatedResponse<T> => {
  const raw = unwrapApiPayload<unknown>(payload);

  if (Array.isArray(raw)) {
    return {
      content: raw as T[],
      page: 0,
      size: raw.length,
      totalElements: raw.length,
      totalPages: 1,
      first: true,
      last: true
    };
  }

  const source = (raw ?? {}) as ApiRecord;
  const content = Array.isArray(source.content) ? (source.content as T[]) : [];
  const page = Number(source.page ?? source.number ?? 0);
  const size = Number(source.size ?? content.length);
  const totalElements = Number(source.totalElements ?? source.total ?? content.length);
  const totalPages = Number(source.totalPages ?? 1);

  return {
    content,
    page,
    size,
    totalElements,
    totalPages,
    first: page === 0,
    last: page + 1 >= totalPages
  };
};

export const apiClient = axios.create({
  baseURL: API_BASE_URL,
  withCredentials: true,
  timeout: 15000,
  headers: {
    "Content-Type": "application/json"
  }
});

apiClient.interceptors.request.use((config) => {
  const token = getAccessToken();

  if (token) {
    config.headers = config.headers ?? {};
    (config.headers as Record<string, string>).Authorization = `Bearer ${token}`;
  }

  return config;
});

apiClient.interceptors.response.use(
  (response) => response,
  (error: AxiosError) => {
    if (error.response?.status === 401 && typeof window !== "undefined") {
      const authStore = useAuthStore.getState();

      authStore.clearSession();
      clearAuthCookies();
      toast.error("Session expired. Please log in again.");

      if (!window.location.pathname.startsWith("/login")) {
        window.location.href = "/login";
      }
    }

    return Promise.reject(error);
  }
);

export const getApiErrorMessage = (error: unknown, fallback: string): string => {
  if (axios.isAxiosError(error)) {
    const responseData = error.response?.data;

    if (typeof responseData === "string" && responseData.trim().length > 0) {
      return responseData;
    }

    if (isRecord(responseData)) {
      const message = responseData.message;
      const errorText = responseData.error;

      if (typeof message === "string" && message.trim().length > 0) {
        return message;
      }

      if (typeof errorText === "string" && errorText.trim().length > 0) {
        return errorText;
      }
    }
  }

  if (error instanceof Error && error.message.trim().length > 0) {
    return error.message;
  }

  return fallback;
};

export const authApi = {
  login: async (payload: LoginPayload): Promise<AuthSession> => {
    const response = await apiClient.post("/api/v1/auth/login", payload);
    const raw = unwrapApiPayload<unknown>(response.data);

    return normalizeAuthSession(raw);
  },
  register: async (payload: RegisterPayload): Promise<AuthSession> => {
    const response = await apiClient.post("/api/v1/auth/register", payload);
    const raw = unwrapApiPayload<unknown>(response.data);

    return normalizeAuthSession(raw);
  }
};

export const accountsApi = {
  getAll: async (): Promise<Account[]> => {
    const response = await apiClient.get("/api/v1/accounts");
    const raw = unwrapApiPayload<unknown>(response.data);

    if (Array.isArray(raw)) {
      return raw as Account[];
    }

    if (isRecord(raw) && Array.isArray(raw.content)) {
      return raw.content as Account[];
    }

    return [];
  },
  create: async (payload: CreateAccountPayload): Promise<Account> => {
    const response = await apiClient.post("/api/v1/accounts", payload);

    return unwrapApiPayload<Account>(response.data);
  }
};

export const transactionsApi = {
  getAll: async (filters: TransactionFilters): Promise<PaginatedResponse<Transaction>> => {
    const response = await apiClient.get("/api/v1/transactions", {
      params: {
        page: filters.page ?? 0,
        size: filters.size ?? 10,
        type: filters.type || undefined,
        startDate: filters.startDate || undefined,
        endDate: filters.endDate || undefined,
        accountId: filters.accountId || undefined
      }
    });

    return normalizePaginatedResponse<Transaction>(response.data);
  },
  transfer: async (payload: TransferPayload, idempotencyKey: string): Promise<Transaction> => {
    const response = await apiClient.post("/api/v1/transactions/transfer", payload, {
      headers: {
        "Idempotency-Key": idempotencyKey
      }
    });

    return unwrapApiPayload<Transaction>(response.data);
  }
};

export const customersApi = {
  getAll: async (): Promise<Customer[]> => {
    const response = await apiClient.get("/api/v1/customers");
    const raw = unwrapApiPayload<unknown>(response.data);

    if (Array.isArray(raw)) {
      return raw as Customer[];
    }

    if (isRecord(raw) && Array.isArray(raw.content)) {
      return raw.content as Customer[];
    }

    return [];
  }
};


