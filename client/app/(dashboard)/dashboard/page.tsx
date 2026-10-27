"use client";

import { useQuery } from "@tanstack/react-query";
import Link from "next/link";
import { ArrowLeftRight, Sparkles } from "lucide-react";

import { Amount, EmptyState } from "@/components/form";
import { SectionHeader } from "@/components/section-header";
import { TransactionList } from "@/components/transaction-list";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { aiApi } from "@/lib/api";
import { useMyAccounts, useMyTransactions } from "@/lib/hooks";
import { useAuthStore } from "@/store/auth-store";

export default function DashboardPage() {
  const user = useAuthStore((s) => s.user);
  const accounts = useMyAccounts();
  const transactions = useMyTransactions({ page: 0, size: 6 });
  const verified = accounts.customer?.kycStatus === "VERIFIED";
  const insights = useQuery({
    queryKey: ["ai", "insights"],
    queryFn: aiApi.insights,
    enabled: verified,
    staleTime: 60 * 60 * 1000,
    retry: false
  });

  const list = accounts.data ?? [];
  const total = list.filter((a) => a.status !== "CLOSED").reduce((sum, a) => sum + Number(a.balance), 0);
  const ids = new Set(list.map((a) => a.id));

  return (
    <div className="space-y-6">
      <SectionHeader
        title={`Hi, ${user?.fullName.split(" ")[0] ?? "there"}`}
        description="Here's where your money stands today."
        action={
          verified ? (
            <Link href="/transfer">
              <Button className="gap-2"><ArrowLeftRight className="h-4 w-4" /> Pay or transfer</Button>
            </Link>
          ) : null
        }
      />

      {accounts.customer && !verified ? (
        <Card className="border-accent/30 bg-accent/5">
          <CardContent className="text-sm text-slate">
            Your identity verification is <strong className="text-ink">{accounts.customer.kycStatus.toLowerCase()}</strong>.
            You&apos;ll be able to open accounts and make payments once it&apos;s approved.
          </CardContent>
        </Card>
      ) : null}

      <div className="grid gap-4 md:grid-cols-3">
        <Card>
          <CardHeader><CardTitle className="text-base">Total balance</CardTitle></CardHeader>
          <CardContent>
            {accounts.isLoading ? <Skeleton className="h-9 w-40" /> : (
              <p className="text-3xl"><Amount value={total} /></p>
            )}
          </CardContent>
        </Card>
        <Card className="md:col-span-2">
          <CardHeader>
            <div>
              <CardTitle className="flex items-center gap-2 text-base">
                <Sparkles className="h-4 w-4 text-primary" /> This month
              </CardTitle>
              <CardDescription>Insights written from your own transactions.</CardDescription>
            </div>
          </CardHeader>
          <CardContent>
            {!verified ? (
              <p className="text-sm text-slate">Available once your account is active.</p>
            ) : insights.isLoading ? (
              <Skeleton className="h-16 w-full" />
            ) : insights.data?.insights.length ? (
              <ul className="list-disc space-y-1 pl-5 text-sm text-ink">
                {insights.data.insights.map((text) => <li key={text}>{text}</li>)}
              </ul>
            ) : (
              <p className="text-sm text-slate">Not enough activity yet for insights.</p>
            )}
          </CardContent>
        </Card>
      </div>

      <div className="grid gap-6 lg:grid-cols-[1.3fr_0.7fr]">
        <Card>
          <CardHeader>
            <CardTitle>Recent activity</CardTitle>
            <Link href="/transactions" className="text-sm font-semibold text-primary">View all</Link>
          </CardHeader>
          <CardContent>
            {transactions.isLoading ? <Skeleton className="h-40 w-full" /> : (
              <TransactionList transactions={transactions.data?.content ?? []} myAccountIds={ids} />
            )}
          </CardContent>
        </Card>
        <Card>
          <CardHeader>
            <CardTitle>Accounts</CardTitle>
            <Link href="/accounts" className="text-sm font-semibold text-primary">Manage</Link>
          </CardHeader>
          <CardContent className="space-y-3">
            {accounts.isLoading ? <Skeleton className="h-24 w-full" /> : list.length === 0 ? (
              <EmptyState title="No accounts yet">
                {verified ? <Link href="/accounts" className="text-primary">Open your first account</Link> : null}
              </EmptyState>
            ) : (
              list.map((a) => (
                <Link key={a.id} href={`/accounts/${a.id}`}
                  className="flex items-center justify-between rounded-xl border border-line p-3 hover:border-primary/40">
                  <div>
                    <p className="text-sm font-medium text-ink">{a.accountType === "CHECKING" ? "Cheque" : "Savings"}</p>
                    <p className="text-xs text-slate">{a.accountNumber}</p>
                  </div>
                  <div className="text-right">
                    <Amount value={Number(a.balance)} />
                    {a.status !== "ACTIVE" ? <Badge variant="danger" className="ml-2">{a.status}</Badge> : null}
                  </div>
                </Link>
              ))
            )}
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
