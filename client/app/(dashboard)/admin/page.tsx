"use client";

import { useMemo } from "react";

import { CreateAccountForm } from "@/components/create-account-form";
import { SectionHeader } from "@/components/section-header";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { useAccountsQuery, useCustomersQuery, useTransactionsQuery } from "@/lib/hooks";
import { formatCurrency, formatDateTime } from "@/lib/utils";
import { useAuthStore } from "@/store/auth-store";

export default function AdminPage() {
  const user = useAuthStore((state) => state.user);
  const isAllowed = user?.role === "ADMIN" || user?.role === "SUPPORT";

  const customersQuery = useCustomersQuery(isAllowed);
  const accountsQuery = useAccountsQuery();
  const transactionsQuery = useTransactionsQuery({ page: 0, size: 50 });

  const transactionVolume = useMemo(() => {
    return (transactionsQuery.data?.content ?? []).reduce(
      (sum, transaction) => sum + Math.abs(transaction.amount ?? 0),
      0
    );
  }, [transactionsQuery.data?.content]);

  if (!isAllowed) {
    return (
      <Card>
        <CardHeader>
          <CardTitle>Access restricted</CardTitle>
          <CardDescription>You do not have permission to access the admin workspace.</CardDescription>
        </CardHeader>
      </Card>
    );
  }

  const loading = customersQuery.isLoading || accountsQuery.isLoading || transactionsQuery.isLoading;

  return (
    <div>
      <SectionHeader
        title="Admin panel"
        description="Customer oversight, account provisioning, and audit summary."
      />

      <div className="grid gap-4 md:grid-cols-3">
        {loading ? (
          <>
            <Skeleton className="h-28 w-full" />
            <Skeleton className="h-28 w-full" />
            <Skeleton className="h-28 w-full" />
          </>
        ) : (
          <>
            <Card>
              <CardHeader>
                <CardTitle className="text-base">Customers</CardTitle>
              </CardHeader>
              <CardContent>
                <p className="text-3xl font-semibold text-ink">{customersQuery.data?.length ?? 0}</p>
                <p className="text-xs text-slate">Registered profiles</p>
              </CardContent>
            </Card>

            <Card>
              <CardHeader>
                <CardTitle className="text-base">Accounts</CardTitle>
              </CardHeader>
              <CardContent>
                <p className="text-3xl font-semibold text-ink">{accountsQuery.data?.length ?? 0}</p>
                <p className="text-xs text-slate">Open products</p>
              </CardContent>
            </Card>

            <Card>
              <CardHeader>
                <CardTitle className="text-base">Audit volume</CardTitle>
              </CardHeader>
              <CardContent>
                <p className="text-3xl font-semibold text-ink">{formatCurrency(transactionVolume, "ZAR")}</p>
                <p className="text-xs text-slate">Across latest 50 transactions</p>
              </CardContent>
            </Card>
          </>
        )}
      </div>

      {user?.role === "ADMIN" ? <CreateAccountForm className="mt-6" /> : null}

      <Card className="mt-6">
        <CardHeader>
          <div>
            <CardTitle>Customers</CardTitle>
            <CardDescription>Customer directory sourced from `/api/v1/customers`.</CardDescription>
          </div>
        </CardHeader>
        <CardContent>
          {customersQuery.isLoading ? (
            <div className="space-y-3">
              <Skeleton className="h-12 w-full" />
              <Skeleton className="h-12 w-full" />
              <Skeleton className="h-12 w-full" />
            </div>
          ) : (customersQuery.data?.length ?? 0) === 0 ? (
            <p className="text-sm text-slate">No customers were returned by the service.</p>
          ) : (
            <div className="table-shell">
              <table className="table-base">
                <thead>
                  <tr>
                    <th>Name</th>
                    <th>Email</th>
                    <th>Phone</th>
                    <th>Joined</th>
                  </tr>
                </thead>
                <tbody>
                  {customersQuery.data?.map((customer) => (
                    <tr key={customer.id}>
                      <td>
                        {`${customer.firstName || ""} ${customer.lastName || ""}`.trim() || "Unnamed customer"}
                      </td>
                      <td>{customer.email}</td>
                      <td>{customer.phoneNumber || "-"}</td>
                      <td>{customer.createdAt ? formatDateTime(customer.createdAt) : "-"}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  );
}

