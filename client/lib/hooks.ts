"use client";

import { keepPreviousData, useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import toast from "react-hot-toast";

import { accountsApi, authApi, customersApi, getApiErrorMessage, transactionsApi } from "@/lib/api";
import { useAuthStore } from "@/store/auth-store";
import type { CreateAccountPayload } from "@/types/account";
import type { LoginPayload, RegisterPayload } from "@/types/auth";
import type { TransactionFilters, TransferPayload } from "@/types/transaction";

export const queryKeys = {
  accounts: ["accounts"] as const,
  customers: ["customers"] as const,
  transactions: (filters: TransactionFilters) => ["transactions", filters] as const
};

export const useLoginMutation = () => {
  return useMutation({
    mutationFn: (payload: LoginPayload) => authApi.login(payload),
    onSuccess: (session) => {
      useAuthStore.getState().setSession(session);
      toast.success("Signed in successfully.");
    },
    onError: (error) => {
      toast.error(getApiErrorMessage(error, "Unable to sign in."));
    }
  });
};

export const useRegisterMutation = () => {
  return useMutation({
    mutationFn: (payload: RegisterPayload) => authApi.register(payload),
    onSuccess: (session) => {
      useAuthStore.getState().setSession(session);
      toast.success("Account created successfully.");
    },
    onError: (error) => {
      toast.error(getApiErrorMessage(error, "Unable to register account."));
    }
  });
};

export const useAccountsQuery = () => {
  return useQuery({
    queryKey: queryKeys.accounts,
    queryFn: accountsApi.getAll,
    staleTime: 30000
  });
};

export const useCustomersQuery = (enabled = true) => {
  return useQuery({
    queryKey: queryKeys.customers,
    queryFn: customersApi.getAll,
    staleTime: 30000,
    enabled
  });
};

export const useTransactionsQuery = (filters: TransactionFilters) => {
  return useQuery({
    queryKey: queryKeys.transactions(filters),
    queryFn: () => transactionsApi.getAll(filters),
    placeholderData: keepPreviousData,
    staleTime: 15000
  });
};

export const useTransferMutation = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ payload, idempotencyKey }: { payload: TransferPayload; idempotencyKey: string }) =>
      transactionsApi.transfer(payload, idempotencyKey),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.accounts });
      queryClient.invalidateQueries({ queryKey: ["transactions"] });
      toast.success("Transfer completed.");
    },
    onError: (error) => {
      toast.error(getApiErrorMessage(error, "Transfer failed."));
    }
  });
};

export const useCreateAccountMutation = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (payload: CreateAccountPayload) => accountsApi.create(payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.accounts });
      toast.success("Account created successfully.");
    },
    onError: (error) => {
      toast.error(getApiErrorMessage(error, "Unable to create account."));
    }
  });
};

