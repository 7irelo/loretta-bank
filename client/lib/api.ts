import axios, { AxiosError } from "axios";

import type {
  Account,
  AssistantReply,
  AuditLog,
  Customer,
  HelpAnswer,
  Insights,
  KycStatus,
  NotificationItem,
  PaginatedResponse,
  PaymentProposal,
  Reconciliation,
  SearchResult,
  SessionUser,
  Statement,
  Transaction,
  TransactionType,
  Transfer
} from "@/types/api";

/**
 * All calls go to same-origin route handlers. Tokens live in HttpOnly cookies set by those
 * handlers, so nothing here ever touches a credential.
 */
export const api = axios.create({
  baseURL: "/api/proxy",
  timeout: 30000,
  withCredentials: true,
  headers: { "Content-Type": "application/json", "x-loretta-csrf": "1" }
});

const auth = axios.create({
  baseURL: "/api/auth",
  timeout: 15000,
  withCredentials: true,
  headers: { "Content-Type": "application/json", "x-loretta-csrf": "1" }
});

api.interceptors.response.use(
  (response) => response,
  (error: AxiosError) => {
    if (error.response?.status === 401 && typeof window !== "undefined") {
      if (!window.location.pathname.startsWith("/login")) {
        window.location.href = `/login?from=${encodeURIComponent(window.location.pathname)}`;
      }
    }
    return Promise.reject(error);
  }
);

export const isStepUpRequired = (error: unknown): boolean =>
  axios.isAxiosError(error) && error.response?.status === 428;

export const errorMessage = (error: unknown, fallback: string): string => {
  if (axios.isAxiosError(error)) {
    const data = error.response?.data as { message?: string; error?: string } | undefined;
    if (data?.message) {
      return data.message;
    }
    if (error.response?.status === 429) {
      return "Too many requests. Please wait a moment and try again.";
    }
  }
  return fallback;
};

export const newIdempotencyKey = (): string =>
  typeof crypto !== "undefined" && "randomUUID" in crypto
    ? crypto.randomUUID()
    : `${Date.now()}-${Math.random().toString(36).slice(2)}`;

const data = <T>(promise: Promise<{ data: T }>): Promise<T> => promise.then((r) => r.data);

export const authApi = {
  login: (email: string, password: string) =>
    data<{ user: SessionUser }>(auth.post("/login", { email, password })),
  register: (fullName: string, email: string, password: string) =>
    data<{ user: SessionUser }>(auth.post("/register", { fullName, email, password })),
  logout: () => data(auth.post("/logout")),
  stepUp: (password: string) => data(auth.post("/step-up", { password }))
};

export const customersApi = {
  me: () => data<Customer>(api.get("/api/v1/customers/me")),
  create: (body: Omit<Customer, "id" | "kycStatus" | "createdAt">) =>
    data<Customer>(api.post("/api/v1/customers", body)),
  update: (id: number, body: Partial<Pick<Customer, "phoneNumber" | "address">>) =>
    data<Customer>(api.put(`/api/v1/customers/${id}`, body)),
  list: (page = 0, kycStatus?: KycStatus) =>
    data<PaginatedResponse<Customer>>(
      api.get("/api/v1/customers", { params: { page, size: 20, kycStatus } })
    ),
  setKyc: (id: number, status: KycStatus) =>
    data<Customer>(api.patch(`/api/v1/customers/${id}/kyc`, null, { params: { status } }))
};

export const accountsApi = {
  mine: (customerId: number) => data<Account[]>(api.get(`/api/v1/accounts/customer/${customerId}`)),
  get: (id: number) => data<Account>(api.get(`/api/v1/accounts/${id}`)),
  open: (customerId: number, accountType: Account["accountType"]) =>
    data<Account>(api.post("/api/v1/accounts", { customerId, accountType, currency: "ZAR" })),
  list: (page = 0) =>
    data<PaginatedResponse<Account>>(api.get("/api/v1/accounts", { params: { page, size: 20 } })),
  action: (id: number, action: "freeze" | "unfreeze" | "close") =>
    data<Account>(api.patch(`/api/v1/accounts/${id}/${action}`)),
  reconciliation: () => data<Reconciliation>(api.get("/api/v1/accounts/admin/reconciliation"))
};

export interface TransactionQuery {
  page?: number;
  size?: number;
  type?: TransactionType | "";
  from?: string;
  to?: string;
  q?: string;
}

const cleanQuery = (query: TransactionQuery) =>
  Object.fromEntries(Object.entries(query).filter(([, v]) => v !== undefined && v !== ""));

export const transactionsApi = {
  mine: (query: TransactionQuery) =>
    data<PaginatedResponse<Transaction>>(
      api.get("/api/v1/transactions/mine", { params: cleanQuery(query) })
    ),
  forAccount: (accountId: number, query: TransactionQuery) =>
    data<PaginatedResponse<Transaction>>(
      api.get(`/api/v1/transactions/account/${accountId}`, { params: cleanQuery(query) })
    ),
  transfer: (
    body: {
      sourceAccountId: number;
      targetAccountId?: number;
      targetAccountNumber?: string;
      amount: number;
      description?: string;
    },
    idempotencyKey: string
  ) =>
    data<Transfer>(
      api.post("/api/v1/transfers", { ...body, currency: "ZAR" }, {
        headers: { "Idempotency-Key": idempotencyKey }
      })
    ),
  deposit: (accountId: number, amount: number, description: string, idempotencyKey: string) =>
    data<Transaction>(
      api.post(
        "/api/v1/transactions/deposit",
        { accountId, amount, currency: "ZAR", description },
        { headers: { "Idempotency-Key": idempotencyKey } }
      )
    )
};

export const notificationsApi = {
  list: (page = 0) =>
    data<PaginatedResponse<NotificationItem>>(
      api.get("/api/v1/notifications", { params: { page, size: 20 } })
    ),
  unread: () => data<{ unread: number }>(api.get("/api/v1/notifications/unread-count")),
  markRead: (id: number) => data(api.patch(`/api/v1/notifications/${id}/read`)),
  markAllRead: () => data(api.post("/api/v1/notifications/read-all"))
};

export const reportsApi = {
  generate: (accountId: number, periodFrom: string, periodTo: string) =>
    data<Statement>(api.post("/api/v1/reports/statements", { accountId, periodFrom, periodTo })),
  forAccount: (accountId: number) =>
    data<Statement[]>(api.get(`/api/v1/reports/statements/account/${accountId}`))
};

export const auditApi = {
  search: (page = 0, eventType?: string) =>
    data<PaginatedResponse<AuditLog>>(
      api.get("/api/v1/audit", { params: { page, size: 20, eventType: eventType || undefined } })
    )
};

export const usersApi = {
  list: () =>
    data<{ id: number; email: string; firstName: string; lastName: string; role: string; enabled: boolean }[]>(
      api.get("/api/v1/auth/users")
    ),
  setRole: (id: number, role: string) => data(api.put(`/api/v1/auth/users/${id}/role`, { role }))
};

export const aiApi = {
  chat: (message: string, history: { role: "user" | "assistant"; text: string }[]) =>
    data<AssistantReply>(api.post("/api/v1/ai/assistant", { message, history })),
  proposal: (id: string) => data<PaymentProposal>(api.get(`/api/v1/ai/proposals/${id}`)),
  search: (question: string) => data<SearchResult>(api.post("/api/v1/ai/search", { question })),
  help: (question: string) => data<HelpAnswer>(api.post("/api/v1/ai/help", { question })),
  insights: () => data<Insights>(api.get("/api/v1/ai/insights")),
  voice: (audio: Blob) => {
    const form = new FormData();
    form.append("audio", audio, "speech.webm");
    return data<{ transcript: string; reply: AssistantReply; audio_base64?: string | null }>(
      api.post("/api/v1/ai/voice", form, { headers: { "Content-Type": "multipart/form-data" } })
    );
  }
};
