"use client";

import Link from "next/link";
import { ArrowLeftRight, ReceiptText, Wallet } from "lucide-react";

import { SectionHeader } from "@/components/section-header";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { useAccountsQuery, useTransactionsQuery } from "@/lib/hooks";
import { formatCurrency, formatDateTime } from "@/lib/utils";
import { useAuthStore } from "@/store/auth-store";

export default function DashboardPage() {
  const user = useAuthStore((state) => state.user);
  const accountsQuery = useAccountsQuery();
  const transactionsQuery = useTransactionsQuery({ page: 0, size: 5 });

  const accounts = accountsQuery.data ?? [];
  const transactions = transactionsQuery.data?.content ?? [];

  const totalBalance = accounts.reduce((sum, account) => sum + Number(account.balance ?? 0), 0);
  const recentVolume = transactions.reduce((sum, transaction) => sum + Math.abs(transaction.amount ?? 0), 0);

  const loading = accountsQuery.isLoading || transactionsQuery.isLoading;

  return (
    <div>
      <SectionHeader
        title={`Welcome, ${user?.fullName ?? "Customer"}`}
        description="Monitor your balances and recent activity in one consolidated view."
        action={
          <Link href="/transfer">
            <Button className="gap-2">
              <ArrowLeftRight className="h-4 w-4" />
              Quick transfer
            </Button>
          </Link>
        }
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
                <CardTitle className="text-base">Total balance</CardTitle>
              </CardHeader>
              <CardContent>
                <p className="text-3xl font-semibold text-ink">{formatCurrency(totalBalance, "ZAR")}</p>
                <p className="text-xs text-slate">Across all active accounts</p>
              </CardContent>
            </Card>

            <Card>
              <CardHeader>
                <CardTitle className="text-base">Open accounts</CardTitle>
              </CardHeader>
              <CardContent>
                <p className="text-3xl font-semibold text-ink">{accounts.length}</p>
                <p className="text-xs text-slate">Retail + business products</p>
              </CardContent>
            </Card>

            <Card>
              <CardHeader>
                <CardTitle className="text-base">Recent flow</CardTitle>
              </CardHeader>
              <CardContent>
                <p className="text-3xl font-semibold text-ink">{formatCurrency(recentVolume, "ZAR")}</p>
                <p className="text-xs text-slate">Latest 5 transactions</p>
              </CardContent>
            </Card>
          </>
        )}
      </div>

      <div className="mt-6 grid gap-6 lg:grid-cols-[1.2fr_0.8fr]">
        <Card>
          <CardHeader>
            <div>
              <CardTitle>Recent transactions</CardTitle>
              <CardDescription>Most recent entries synced from the transactions service.</CardDescription>
            </div>
            <Link href="/transactions" className="text-sm font-semibold text-primary">
              View all
            </Link>
          </CardHeader>
          <CardContent>
            {transactionsQuery.isLoading ? (
              <div className="space-y-3">
                <Skeleton className="h-14 w-full" />
                <Skeleton className="h-14 w-full" />
                <Skeleton className="h-14 w-full" />
              </div>
            ) : transactions.length === 0 ? (
              <p className="text-sm text-slate">No transactions found yet.</p>
            ) : (
              <div className="space-y-3">
                {transactions.map((transaction) => (
                  <div
                    key={transaction.id}
                    className="flex items-start justify-between rounded-xl border border-line bg-surface/70 p-3"
                  >
                    <div>
                      <p className="font-medium text-ink">{transaction.description || transaction.type}</p>
                      <p className="text-xs text-slate">{formatDateTime(transaction.createdAt)}</p>
                    </div>
                    <div className="text-right">
                      <p className="font-semibold text-ink">
                        {formatCurrency(transaction.amount, transaction.currency || "ZAR")}
                      </p>
                      <Badge variant="soft" className="mt-1">
                        {transaction.type}
                      </Badge>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <div>
              <CardTitle>Your accounts</CardTitle>
              <CardDescription>Quick links to active accounts.</CardDescription>
            </div>
            <Link href="/accounts" className="text-sm font-semibold text-primary">
              Manage
            </Link>
          </CardHeader>
          <CardContent>
            {accountsQuery.isLoading ? (
              <div className="space-y-3">
                <Skeleton className="h-16 w-full" />
                <Skeleton className="h-16 w-full" />
              </div>
            ) : accounts.length === 0 ? (
              <p className="text-sm text-slate">No accounts available.</p>
            ) : (
              <div className="space-y-3">
                {accounts.slice(0, 4).map((account) => (
                  <Link
                    key={account.id}
                    href={`/accounts/${account.id}`}
                    className="flex items-center justify-between rounded-xl border border-line bg-surface p-3 transition hover:border-primary/40 hover:bg-white"
                  >
                    <div>
                      <p className="text-sm font-medium text-ink">{account.accountType}</p>
                      <p className="text-xs text-slate">{account.accountNumber}</p>
                    </div>
                    <div className="text-right">
                      <p className="text-sm font-semibold text-ink">
                        {formatCurrency(account.balance, account.currency || "ZAR")}
                      </p>
                      <Badge variant="soft" className="mt-1">
                        {account.status || "ACTIVE"}
                      </Badge>
                    </div>
                  </Link>
                ))}
              </div>
            )}
          </CardContent>
        </Card>
      </div>

      <div className="mt-6 rounded-2xl border border-line bg-white/90 p-4">
        <div className="flex items-center gap-2 text-sm text-slate">
          <Wallet className="h-4 w-4" />
          <span>Need to move money quickly?</span>
          <Link href="/transfer" className="font-semibold text-primary">
            Start transfer
          </Link>
        </div>
        <div className="mt-2 flex items-center gap-2 text-sm text-slate">
          <ReceiptText className="h-4 w-4" />
          <span>Audit-ready history is available in the transactions section.</span>
        </div>
      </div>
    </div>
  );
}

