"use client";

import { useMemo, useState } from "react";
import { useParams } from "next/navigation";

import { SectionHeader } from "@/components/section-header";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { useAccountsQuery, useTransactionsQuery } from "@/lib/hooks";
import { formatCurrency, formatDateTime } from "@/lib/utils";

const PAGE_SIZE = 10;

export default function AccountDetailPage() {
  const params = useParams<{ id: string }>();
  const accountId = params.id;

  const [page, setPage] = useState(0);
  const accountsQuery = useAccountsQuery();
  const transactionsQuery = useTransactionsQuery({
    page,
    size: PAGE_SIZE,
    accountId
  });

  const account = useMemo(
    () => (accountsQuery.data ?? []).find((item) => String(item.id) === String(accountId)),
    [accountId, accountsQuery.data]
  );

  if (accountsQuery.isLoading) {
    return (
      <div className="space-y-4">
        <Skeleton className="h-8 w-44" />
        <Skeleton className="h-28 w-full" />
        <Skeleton className="h-72 w-full" />
      </div>
    );
  }

  if (!account) {
    return (
      <Card>
        <CardContent>
          <p className="text-sm text-slate">Account not found or unavailable for your profile.</p>
        </CardContent>
      </Card>
    );
  }

  const pageData = transactionsQuery.data;
  const rows = pageData?.content ?? [];
  const totalPages = pageData?.totalPages ?? 1;

  return (
    <div>
      <SectionHeader
        title={`${account.accountType} account`}
        description={`Account number ${account.accountNumber}`}
      />

      <Card className="mb-6">
        <CardHeader>
          <div>
            <CardTitle>Account overview</CardTitle>
            <CardDescription>Current balance and metadata from the accounts service.</CardDescription>
          </div>
        </CardHeader>
        <CardContent className="grid gap-4 md:grid-cols-3">
          <div>
            <p className="text-xs uppercase tracking-wide text-muted">Balance</p>
            <p className="mt-1 text-3xl font-semibold text-ink">
              {formatCurrency(account.balance, account.currency || "ZAR")}
            </p>
          </div>
          <div>
            <p className="text-xs uppercase tracking-wide text-muted">Status</p>
            <p className="mt-1 text-sm font-semibold text-ink">{account.status || "ACTIVE"}</p>
          </div>
          <div>
            <p className="text-xs uppercase tracking-wide text-muted">Opened</p>
            <p className="mt-1 text-sm font-semibold text-ink">{formatDateTime(account.createdAt)}</p>
          </div>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <div>
            <CardTitle>Transaction history</CardTitle>
            <CardDescription>Filtered by this account identifier.</CardDescription>
          </div>
        </CardHeader>
        <CardContent>
          {transactionsQuery.isLoading ? (
            <div className="space-y-3">
              <Skeleton className="h-14 w-full" />
              <Skeleton className="h-14 w-full" />
              <Skeleton className="h-14 w-full" />
            </div>
          ) : rows.length === 0 ? (
            <p className="text-sm text-slate">No transactions found for this account.</p>
          ) : (
            <div className="table-shell">
              <table className="table-base">
                <thead>
                  <tr>
                    <th>Date</th>
                    <th>Type</th>
                    <th>Description</th>
                    <th className="text-right">Amount</th>
                  </tr>
                </thead>
                <tbody>
                  {rows.map((transaction) => (
                    <tr key={transaction.id}>
                      <td>{formatDateTime(transaction.createdAt)}</td>
                      <td>{transaction.type}</td>
                      <td>{transaction.description || transaction.reference || "-"}</td>
                      <td className="text-right font-semibold text-ink">
                        {formatCurrency(transaction.amount, transaction.currency || "ZAR")}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}

          <div className="mt-4 flex items-center justify-between">
            <p className="text-xs text-slate">
              Page {page + 1} of {Math.max(totalPages, 1)}
            </p>
            <div className="flex items-center gap-2">
              <Button variant="secondary" size="sm" disabled={page === 0} onClick={() => setPage((prev) => prev - 1)}>
                Previous
              </Button>
              <Button
                variant="secondary"
                size="sm"
                disabled={page + 1 >= totalPages}
                onClick={() => setPage((prev) => prev + 1)}
              >
                Next
              </Button>
            </div>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
