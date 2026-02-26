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

const asString = (value: unknown): string => (typeof value === "string" ? value : "");

const asNumber = (value: unknown, fallback = 0): number => {
  if (typeof value === "number" && Number.isFinite(value)) {
    return value;
  }

  if (typeof value === "string" && value.trim().length > 0) {
    const parsed = Number(value);
    if (Number.isFinite(parsed)) {
      return parsed;
    }
  }

  return fallback;
};

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

const normalizeAccount = (raw: unknown): Account => {
  const source = (raw ?? {}) as ApiRecord;

  return {
    id: String(source.id ?? ""),
    accountNumber: asString(source.accountNumber),
    accountType: asString(source.accountType),
    balance: asNumber(source.balance),
    currency: asString(source.currency) || "ZAR",
    customerId: String(source.customerId ?? ""),
    status: asString(source.status) || "ACTIVE",
    createdAt: asString(source.createdAt)
  };
};

const normalizeTransaction = (raw: unknown): Transaction => {
  const source = (raw ?? {}) as ApiRecord;

  return {
    id: String(source.id ?? source.transferId ?? ""),
    accountId: String(source.accountId ?? ""),
    sourceAccountId: String(source.sourceAccountId ?? ""),
    targetAccountId: String(source.targetAccountId ?? ""),
    amount: asNumber(source.amount),
    currency: asString(source.currency) || "ZAR",
    type: asString(source.type) || "TRANSFER",
    description: asString(source.description),
    reference: asString(source.reference),
    createdAt: asString(source.createdAt) || new Date().toISOString()
  };
};

const filterTransactions = (transactions: Transaction[], filters: TransactionFilters): Transaction[] => {
  const normalizedType = filters.type?.trim().toUpperCase() ?? "";
  const startAt = filters.startDate ? new Date(`${filters.startDate}T00:00:00`).getTime() : null;
  const endAt = filters.endDate ? new Date(`${filters.endDate}T23:59:59.999`).getTime() : null;

  return transactions.filter((transaction) => {
    if (normalizedType && transaction.type.toUpperCase() !== normalizedType) {
      return false;
    }

    const createdAt = new Date(transaction.createdAt).getTime();
    if (startAt !== null && createdAt < startAt) {
      return false;
    }

    if (endAt !== null && createdAt > endAt) {
      return false;
    }

    return true;
  });
};

const paginateTransactions = (
  transactions: Transaction[],
  page: number,
  size: number
): PaginatedResponse<Transaction> => {
  const safeSize = Math.max(size, 1);
  const safePage = Math.max(page, 0);
  const totalElements = transactions.length;
  const totalPages = Math.max(Math.ceil(totalElements / safeSize), 1);
  const startIndex = safePage * safeSize;
  const content = transactions.slice(startIndex, startIndex + safeSize);

  return {
    content,
    page: safePage,
    size: safeSize,
    totalElements,
    totalPages,
    first: safePage === 0,
    last: safePage + 1 >= totalPages
  };
};

let cachedCustomerId: string | null = null;

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
      cachedCustomerId = null;

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

const resolveCurrentCustomerId = async (): Promise<string | null> => {
  const role = useAuthStore.getState().user?.role;
  if (role && role !== "CUSTOMER") {
    return null;
  }

  if (cachedCustomerId) {
    return cachedCustomerId;
  }

  const response = await apiClient.get("/api/v1/customers/me");
  const raw = unwrapApiPayload<unknown>(response.data);
  const customerIdValue = (raw as ApiRecord).id;
  const customerId =
    customerIdValue === null || customerIdValue === undefined ? "" : String(customerIdValue);

  if (!customerId) {
    return null;
  }

  cachedCustomerId = customerId;
  return customerId;
};

export const authApi = {
  login: async (payload: LoginPayload): Promise<AuthSession> => {
    const response = await apiClient.post("/api/v1/auth/login", payload);
    const raw = unwrapApiPayload<unknown>(response.data);
    cachedCustomerId = null;

    return normalizeAuthSession(raw);
  },
  register: async (payload: RegisterPayload): Promise<AuthSession> => {
    const [firstName, ...rest] = payload.fullName.trim().split(/\s+/);
    const lastName = rest.join(" ");
    const response = await apiClient.post("/api/v1/auth/register", {
      email: payload.email,
      password: payload.password,
      firstName: firstName || payload.email.split("@")[0],
      lastName: lastName || "Customer"
    });
    const raw = unwrapApiPayload<unknown>(response.data);
    cachedCustomerId = null;

    return normalizeAuthSession(raw);
  }
};

export const customersApi = {
  getMe: async (): Promise<Customer> => {
    const response = await apiClient.get("/api/v1/customers/me");
    const raw = unwrapApiPayload<unknown>(response.data);
    const source = (raw ?? {}) as ApiRecord;

    return {
      id: String(source.id ?? ""),
      firstName: asString(source.firstName),
      lastName: asString(source.lastName),
      email: asString(source.email),
      phoneNumber: asString(source.phoneNumber) || undefined,
      createdAt: asString(source.createdAt) || undefined
    };
  },
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

export const accountsApi = {
  getAll: async (): Promise<Account[]> => {
    const role = useAuthStore.getState().user?.role;

    if (role === "ADMIN" || role === "SUPPORT") {
      const customers = await customersApi.getAll();
      if (customers.length === 0) {
        return [];
      }

      const perCustomerAccounts = await Promise.all(
        customers.map(async (customer) => {
          const response = await apiClient.get(`/api/v1/accounts/customer/${customer.id}`);
          const raw = unwrapApiPayload<unknown>(response.data);
          const rows = Array.isArray(raw) ? raw : [];
          return rows.map((account) => normalizeAccount(account));
        })
      );

      return perCustomerAccounts.flat();
    }

    const customerId = await resolveCurrentCustomerId();
    if (!customerId) {
      return [];
    }

    const response = await apiClient.get(`/api/v1/accounts/customer/${customerId}`);
    const raw = unwrapApiPayload<unknown>(response.data);
    const rows = Array.isArray(raw) ? raw : [];

    return rows.map((account) => normalizeAccount(account));
  },
  create: async (payload: CreateAccountPayload): Promise<Account> => {
    const parsedCustomerId = Number(payload.customerId);
    const requestBody = {
      customerId: Number.isFinite(parsedCustomerId) ? parsedCustomerId : payload.customerId,
      accountType: payload.accountType,
      currency: (payload.currency || "ZAR").toUpperCase(),
      initialDeposit: payload.initialDeposit ?? 0
    };
    const response = await apiClient.post("/api/v1/accounts", requestBody);

    return normalizeAccount(unwrapApiPayload<unknown>(response.data));
  }
};

export const transactionsApi = {
  getAll: async (filters: TransactionFilters): Promise<PaginatedResponse<Transaction>> => {
    const page = filters.page ?? 0;
    const size = filters.size ?? 10;

    if (filters.accountId) {
      const response = await apiClient.get(`/api/v1/transactions/account/${filters.accountId}`, {
        params: { page, size }
      });
      const pageData = normalizePaginatedResponse<unknown>(response.data);
      const normalized = pageData.content.map((transaction) => normalizeTransaction(transaction));
      const filtered = filterTransactions(normalized, filters);

      return paginateTransactions(filtered, page, size);
    }

    const accounts = await accountsApi.getAll();
    if (accounts.length === 0) {
      return paginateTransactions([], page, size);
    }

    const fetchSize = Math.max(size * 3, 50);
    const perAccountPages = await Promise.all(
      accounts.map(async (account) => {
        const response = await apiClient.get(`/api/v1/transactions/account/${account.id}`, {
          params: { page: 0, size: fetchSize }
        });
        const pageData = normalizePaginatedResponse<unknown>(response.data);
        return pageData.content.map((transaction) => normalizeTransaction(transaction));
      })
    );

    const uniqueTransactions = new Map<string, Transaction>();
    for (const row of perAccountPages.flat()) {
      if (row.id) {
        uniqueTransactions.set(row.id, row);
      }
    }

    const merged = Array.from(uniqueTransactions.values()).sort((left, right) => {
      return new Date(right.createdAt).getTime() - new Date(left.createdAt).getTime();
    });
    const filtered = filterTransactions(merged, filters);

    return paginateTransactions(filtered, page, size);
  },
  transfer: async (payload: TransferPayload, idempotencyKey: string): Promise<Transaction> => {
    const response = await apiClient.post(
      "/api/v1/transfers",
      {
        sourceAccountId: Number(payload.fromAccountId),
        targetAccountId: Number(payload.toAccountId),
        amount: payload.amount,
        currency: payload.currency || "ZAR",
        description: payload.description
      },
      {
        headers: {
          "Idempotency-Key": idempotencyKey
        }
      }
    );

    return normalizeTransaction(unwrapApiPayload<unknown>(response.data));
  }
};
