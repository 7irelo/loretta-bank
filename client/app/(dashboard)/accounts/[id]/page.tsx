"use client";

import { useQuery } from "@tanstack/react-query";
import Link from "next/link";
import { useParams } from "next/navigation";
import { useMemo, useState } from "react";

import { Amount } from "@/components/form";
import { SectionHeader } from "@/components/section-header";
import { TransactionList } from "@/components/transaction-list";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { accountsApi } from "@/lib/api";
import { useAccountTransactions, useMyAccounts } from "@/lib/hooks";
import { formatDateTime } from "@/lib/utils";

export default function AccountDetailPage() {
  const params = useParams<{ id: string }>();
  const accountId = Number(params.id);
  const [page, setPage] = useState(0);
  const mine = useMyAccounts();
  const ids = useMemo(() => new Set((mine.data ?? []).map((a) => a.id)), [mine.data]);
  const account = useQuery({ queryKey: ["account", accountId], queryFn: () => accountsApi.get(accountId) });
  const transactions = useAccountTransactions(accountId, { page, size: 20 });

  if (account.isLoading) {
    return <Skeleton className="h-60 w-full" />;
  }
  if (!account.data) {
    return <p className="text-sm text-slate">Account not found.</p>;
  }
  const a = account.data;

  return (
    <div className="space-y-6">
      <SectionHeader
        title={a.accountType === "CHECKING" ? "Cheque account" : "Savings account"}
        description={a.accountNumber}
        action={<Link href={`/statements?account=${a.id}`}><Button variant="secondary">Statements</Button></Link>}
      />
      <Card>
        <CardContent className="grid gap-4 md:grid-cols-3">
          <div><p className="text-xs uppercase text-muted">Balance</p><p className="text-3xl"><Amount value={Number(a.balance)} currency={a.currency} /></p></div>
          <div><p className="text-xs uppercase text-muted">Status</p><Badge variant={a.status === "ACTIVE" ? "success" : "danger"}>{a.status.toLowerCase()}</Badge></div>
          <div><p className="text-xs uppercase text-muted">Opened</p><p className="text-sm text-ink">{formatDateTime(a.createdAt)}</p></div>
        </CardContent>
      </Card>
      <Card>
        <CardHeader><CardTitle>Transactions</CardTitle></CardHeader>
        <CardContent>
          <TransactionList transactions={transactions.data?.content ?? []} myAccountIds={ids} />
          <div className="mt-4 flex justify-end gap-2">
            <Button size="sm" variant="secondary" disabled={page === 0} onClick={() => setPage((p) => p - 1)}>Previous</Button>
            <Button size="sm" variant="secondary" disabled={transactions.data?.last ?? true} onClick={() => setPage((p) => p + 1)}>Next</Button>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
