"use client";

import { keepPreviousData, useQuery } from "@tanstack/react-query";
import axios from "axios";

import {
  accountsApi,
  customersApi,
  notificationsApi,
  transactionsApi,
  type TransactionQuery
} from "@/lib/api";
import { useAuthStore } from "@/store/auth-store";

export const keys = {
  me: ["customer", "me"] as const,
  accounts: ["accounts", "mine"] as const,
  transactions: (q: TransactionQuery) => ["transactions", q] as const,
  accountTransactions: (id: number, q: TransactionQuery) => ["transactions", id, q] as const,
  unread: ["notifications", "unread"] as const
};

/** The signed-in customer's profile, or null when they haven't completed onboarding. */
export const useCustomer = () => {
  const isCustomer = useAuthStore((s) => s.user?.role === "CUSTOMER");
  return useQuery({
    queryKey: keys.me,
    enabled: isCustomer,
    queryFn: async () => {
      try {
        return await customersApi.me();
      } catch (error) {
        if (axios.isAxiosError(error) && error.response?.status === 404) {
          return null;
        }
        throw error;
      }
    },
    staleTime: 60_000
  });
};

export const useMyAccounts = () => {
  const customer = useCustomer();
  const customerId = customer.data?.id;
  const accounts = useQuery({
    queryKey: keys.accounts,
    enabled: Boolean(customerId),
    queryFn: () => accountsApi.mine(customerId as number),
    staleTime: 15_000
  });
  return { ...accounts, customer: customer.data, customerLoading: customer.isLoading };
};

export const useMyTransactions = (query: TransactionQuery, enabled = true) =>
  useQuery({
    queryKey: keys.transactions(query),
    queryFn: () => transactionsApi.mine(query),
    placeholderData: keepPreviousData,
    enabled,
    staleTime: 10_000
  });

export const useAccountTransactions = (accountId: number, query: TransactionQuery) =>
  useQuery({
    queryKey: keys.accountTransactions(accountId, query),
    queryFn: () => transactionsApi.forAccount(accountId, query),
    placeholderData: keepPreviousData,
    enabled: Number.isFinite(accountId)
  });

export const useUnreadCount = () =>
  useQuery({
    queryKey: keys.unread,
    queryFn: notificationsApi.unread,
    refetchInterval: 60_000,
    staleTime: 30_000
  });
