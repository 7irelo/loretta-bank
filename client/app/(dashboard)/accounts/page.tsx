"use client";

import { useQueryClient } from "@tanstack/react-query";
import Link from "next/link";
import { useState } from "react";
import toast from "react-hot-toast";

import { Amount, EmptyState, Select } from "@/components/form";
import { SectionHeader } from "@/components/section-header";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { accountsApi, errorMessage } from "@/lib/api";
import { keys, useMyAccounts } from "@/lib/hooks";
import { formatDateTime } from "@/lib/utils";

export default function AccountsPage() {
  const queryClient = useQueryClient();
  const accounts = useMyAccounts();
  const [type, setType] = useState<"CHECKING" | "SAVINGS">("SAVINGS");
  const [pending, setPending] = useState(false);
  const verified = accounts.customer?.kycStatus === "VERIFIED";

  const open = async () => {
    if (!accounts.customer) {
      return;
    }
    setPending(true);
    try {
      const account = await accountsApi.open(accounts.customer.id, type);
      toast.success(`Account ${account.accountNumber} opened`);
      await queryClient.invalidateQueries({ queryKey: keys.accounts });
    } catch (err) {
      toast.error(errorMessage(err, "Could not open the account"));
    } finally {
      setPending(false);
    }
  };

  return (
    <div className="space-y-6">
      <SectionHeader title="Accounts" description="Your cheque and savings accounts." />
      <Card>
        <CardHeader><CardTitle className="text-base">Open a new account</CardTitle></CardHeader>
        <CardContent className="flex flex-col gap-3 sm:flex-row sm:items-end">
          <div className="sm:w-60">
            <Select value={type} onChange={(e) => setType(e.target.value as "CHECKING" | "SAVINGS")}
              disabled={!verified || pending} aria-label="Account type">
              <option value="SAVINGS">Savings</option>
              <option value="CHECKING">Cheque</option>
            </Select>
          </div>
          <Button onClick={open} disabled={!verified || pending}>{pending ? "Opening…" : "Open account"}</Button>
          {!verified ? <p className="text-sm text-slate">Available once your identity is verified.</p> : null}
        </CardContent>
      </Card>

      {accounts.isLoading ? <Skeleton className="h-40 w-full" /> : (accounts.data ?? []).length === 0 ? (
        <EmptyState title="You don't have any accounts yet" />
      ) : (
        <div className="grid gap-4 md:grid-cols-2">
          {(accounts.data ?? []).map((a) => (
            <Link key={a.id} href={`/accounts/${a.id}`}>
              <Card className="transition hover:border-primary/40">
                <CardContent className="space-y-2">
                  <div className="flex items-center justify-between">
                    <p className="font-medium text-ink">{a.accountType === "CHECKING" ? "Cheque account" : "Savings account"}</p>
                    <Badge variant={a.status === "ACTIVE" ? "success" : "danger"}>{a.status.toLowerCase()}</Badge>
                  </div>
                  <p className="text-sm text-slate">{a.accountNumber}</p>
                  <p className="text-2xl"><Amount value={Number(a.balance)} currency={a.currency} /></p>
                  <p className="text-xs text-muted">Opened {formatDateTime(a.createdAt)}</p>
                </CardContent>
              </Card>
            </Link>
          ))}
        </div>
      )}
    </div>
  );
}
