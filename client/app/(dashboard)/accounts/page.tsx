"use client";

import Link from "next/link";

import { CreateAccountForm } from "@/components/create-account-form";
import { SectionHeader } from "@/components/section-header";
import { Badge } from "@/components/ui/badge";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { useAccountsQuery } from "@/lib/hooks";
import { formatCurrency, formatDateTime } from "@/lib/utils";
import { useAuthStore } from "@/store/auth-store";

export default function AccountsPage() {
  const user = useAuthStore((state) => state.user);
  const accountsQuery = useAccountsQuery();
  const accounts = accountsQuery.data ?? [];

  return (
    <div>
      <SectionHeader
        title="Accounts"
        description="View account balances, status, and account-level activity."
      />

      {user?.role === "ADMIN" ? <CreateAccountForm className="mb-6" /> : null}

      {accountsQuery.isLoading ? (
        <div className="grid gap-4 md:grid-cols-2">
          <Skeleton className="h-40 w-full" />
          <Skeleton className="h-40 w-full" />
          <Skeleton className="h-40 w-full" />
          <Skeleton className="h-40 w-full" />
        </div>
      ) : accounts.length === 0 ? (
        <Card>
          <CardContent>
            <p className="text-sm text-slate">No accounts found for this profile.</p>
          </CardContent>
        </Card>
      ) : (
        <div className="grid gap-4 md:grid-cols-2">
          {accounts.map((account) => (
            <Link key={account.id} href={`/accounts/${account.id}`}>
              <Card className="h-full transition hover:-translate-y-0.5 hover:border-primary/35">
                <CardHeader>
                  <div>
                    <CardTitle className="text-base">{account.accountType}</CardTitle>
                    <p className="text-xs text-slate">{account.accountNumber}</p>
                  </div>
                  <Badge variant="soft">{account.status || "ACTIVE"}</Badge>
                </CardHeader>
                <CardContent>
                  <p className="text-2xl font-semibold text-ink">
                    {formatCurrency(account.balance, account.currency || "ZAR")}
                  </p>
                  <p className="text-xs text-slate">Opened {formatDateTime(account.createdAt)}</p>
                </CardContent>
              </Card>
            </Link>
          ))}
        </div>
      )}
    </div>
  );
}

