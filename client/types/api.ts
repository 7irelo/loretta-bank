export interface PaginatedResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}

export type UserRole = "ADMIN" | "CUSTOMER" | "SUPPORT";

export interface SessionUser {
  id: string;
  email: string;
  fullName: string;
  role: UserRole;
}

export type KycStatus = "PENDING" | "VERIFIED" | "REJECTED";

export interface Customer {
  id: number;
  userId: number;
  firstName: string;
  lastName: string;
  email: string;
  phoneNumber?: string;
  idNumber?: string;
  dateOfBirth?: string;
  address?: string;
  kycStatus: KycStatus;
  createdAt?: string;
}

export type AccountStatus = "ACTIVE" | "FROZEN" | "CLOSED";

export interface Account {
  id: number;
  accountNumber: string;
  customerId: number;
  accountType: "CHECKING" | "SAVINGS";
  currency: string;
  balance: number;
  status: AccountStatus;
  createdAt: string;
}

export type TransactionType = "DEPOSIT" | "WITHDRAWAL" | "TRANSFER";
export type TransactionStatus = "PENDING" | "COMPLETED" | "FAILED" | "REVERSED";

export interface Transaction {
  id: string;
  type: TransactionType;
  status: TransactionStatus;
  sourceAccountId?: number | null;
  targetAccountId?: number | null;
  amount: number;
  currency: string;
  description?: string | null;
  reference?: string | null;
  failureReason?: string | null;
  createdAt: string;
}

export interface Transfer {
  transferId: string;
  status: TransactionStatus;
  sourceAccountId: number;
  targetAccountId: number;
  amount: number;
  currency: string;
  reference?: string;
  failureReason?: string | null;
  createdAt: string;
}

export interface NotificationItem {
  id: number;
  type: string;
  subject: string;
  body: string;
  createdAt: string;
  readAt?: string | null;
}

export interface StatementLine {
  transactionId: string;
  createdAt: string;
  type: string;
  direction: "CREDIT" | "DEBIT";
  amount: number;
  currency: string;
  description?: string;
  reference?: string;
}

export interface Statement {
  id: number;
  accountId: number;
  accountNumber: string;
  currency: string;
  periodFrom: string;
  periodTo: string;
  openingBalance: number;
  closingBalance: number;
  totalCredits: number;
  totalDebits: number;
  transactionCount: number;
  generatedAt: string;
  lineItems: StatementLine[];
}

export interface AuditLog {
  id: number;
  eventId: string;
  eventType: string;
  aggregateType: string;
  aggregateId: string;
  occurredAt: string;
}

export interface Reconciliation {
  checkedAt: string;
  ledgerTotal: number;
  accountsWithDrift: number[];
  healthy: boolean;
}

export interface PaymentProposal {
  id: string;
  from_account_id: number;
  from_account_number: string;
  to_account_number: string;
  amount: string;
  description: string;
  expires_at: string;
  warning?: string | null;
}

export interface AssistantReply {
  text: string;
  proposals: PaymentProposal[];
  handoff?: string | null;
  tools_used: string[];
  model: string;
}

export interface Insights {
  month: string;
  insights: string[];
  facts: {
    total_spent: string;
    categories: { category: string; this_month: string; three_month_average: string }[];
  };
}

export interface SearchResult {
  answer: string;
  total?: string | null;
  count: number;
  transactions: Transaction[];
}

export interface HelpAnswer {
  answer: string;
  citations: { document: string; quote: string }[];
  grounded: boolean;
}
