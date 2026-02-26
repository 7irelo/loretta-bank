export interface Account {
  id: string;
  accountNumber: string;
  accountType: string;
  balance: number;
  currency: string;
  customerId: string;
  status: string;
  createdAt: string;
}

export interface CreateAccountPayload {
  customerId: string;
  accountType: string;
  currency?: string;
  initialDeposit?: number;
}

