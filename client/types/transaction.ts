export type TransactionType =
  | "DEBIT"
  | "CREDIT"
  | "TRANSFER"
  | "PAYMENT"
  | "WITHDRAWAL"
  | "DEPOSIT";

export interface Transaction {
  id: string;
  accountId?: string;
  sourceAccountId?: string;
  targetAccountId?: string;
  amount: number;
  currency: string;
  type: TransactionType | string;
  description?: string;
  reference?: string;
  createdAt: string;
}

export interface TransferPayload {
  fromAccountId: string;
  toAccountId: string;
  amount: number;
  currency?: string;
  description?: string;
}

export interface TransactionFilters {
  page?: number;
  size?: number;
  type?: string;
  startDate?: string;
  endDate?: string;
  accountId?: string;
}

