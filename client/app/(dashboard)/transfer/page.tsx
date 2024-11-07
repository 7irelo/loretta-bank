"use client";

import { useMemo, useState, type FormEvent } from "react";
import { z } from "zod";

import { SectionHeader } from "@/components/section-header";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Skeleton } from "@/components/ui/skeleton";
import { useAccountsQuery, useTransferMutation } from "@/lib/hooks";
import { formatCurrency, formatDateTime } from "@/lib/utils";

const transferSchema = z
  .object({
    fromAccountId: z.string().min(1, "Select a source account."),
    toAccountId: z.string().min(1, "Select a destination account."),
    amount: z.coerce.number().positive("Amount must be greater than zero."),
    description: z.string().max(140, "Description must be under 140 characters.").optional()
  })
  .refine((values) => values.fromAccountId !== values.toAccountId, {
    message: "Source and destination accounts must be different.",
    path: ["toAccountId"]
  });

type TransferFormState = {
  fromAccountId: string;
  toAccountId: string;
  amount: string;
  description: string;
};

export default function TransferPage() {
  const accountsQuery = useAccountsQuery();
  const transferMutation = useTransferMutation();
  const [formState, setFormState] = useState<TransferFormState>({
    fromAccountId: "",
    toAccountId: "",
    amount: "",
    description: ""
  });
  const [errors, setErrors] = useState<Partial<Record<keyof TransferFormState, string>>>({});
  const [receipt, setReceipt] = useState<{ idempotencyKey: string; timestamp: string } | null>(null);

  const accounts = useMemo(() => accountsQuery.data ?? [], [accountsQuery.data]);
  const selectedFrom = useMemo(
    () => accounts.find((item) => String(item.id) === formState.fromAccountId),
    [accounts, formState.fromAccountId]
  );

  const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    const parsed = transferSchema.safeParse({
      ...formState,
      amount: formState.amount,
      description: formState.description || undefined
    });

    if (!parsed.success) {
      const fieldErrors = parsed.error.flatten().fieldErrors;

      setErrors({
        fromAccountId: fieldErrors.fromAccountId?.[0],
        toAccountId: fieldErrors.toAccountId?.[0],
        amount: fieldErrors.amount?.[0],
        description: fieldErrors.description?.[0]
      });
      return;
    }

    setErrors({});

    const idempotencyKey =
      typeof crypto !== "undefined" && "randomUUID" in crypto
        ? crypto.randomUUID()
        : `${Date.now()}-${Math.random()}`;

    transferMutation.mutate(
      {
        payload: {
          fromAccountId: parsed.data.fromAccountId,
          toAccountId: parsed.data.toAccountId,
          amount: parsed.data.amount,
          currency: selectedFrom?.currency || "ZAR",
          description: parsed.data.description
        },
        idempotencyKey
      },
      {
        onSuccess: () => {
          setReceipt({ idempotencyKey, timestamp: new Date().toISOString() });
          setFormState({ fromAccountId: "", toAccountId: "", amount: "", description: "" });
        }
      }
    );
  };

  return (
    <div>
      <SectionHeader
        title="Transfer funds"
        description="Move funds across accounts with idempotent submission control."
      />

      <div className="grid gap-6 lg:grid-cols-[1.1fr_0.9fr]">
        <Card>
          <CardHeader>
            <div>
              <CardTitle>New transfer</CardTitle>
              <CardDescription>Each transfer sends an `Idempotency-Key` header to prevent duplicates.</CardDescription>
            </div>
          </CardHeader>
          <CardContent>
            {accountsQuery.isLoading ? (
              <div className="space-y-3">
                <Skeleton className="h-10 w-full" />
                <Skeleton className="h-10 w-full" />
                <Skeleton className="h-10 w-full" />
              </div>
            ) : (
              <form className="space-y-4" onSubmit={handleSubmit}>
                <label className="block space-y-1 text-sm">
                  <span className="font-medium text-slate">From account</span>
                  <select
                    className="h-10 w-full rounded-xl border border-line bg-white px-3 text-sm text-ink outline-none focus:border-primary focus:ring-4 focus:ring-primary/20"
                    value={formState.fromAccountId}
                    onChange={(event) =>
                      setFormState((prev) => ({ ...prev, fromAccountId: event.target.value }))
                    }
                    disabled={transferMutation.isPending}
                  >
                    <option value="">Select account</option>
                    {accounts.map((account) => (
                      <option key={account.id} value={account.id}>
                        {account.accountType} - {account.accountNumber}
                      </option>
                    ))}
                  </select>
                  {errors.fromAccountId ? <p className="text-xs text-danger">{errors.fromAccountId}</p> : null}
                </label>

                <label className="block space-y-1 text-sm">
                  <span className="font-medium text-slate">To account</span>
                  <select
                    className="h-10 w-full rounded-xl border border-line bg-white px-3 text-sm text-ink outline-none focus:border-primary focus:ring-4 focus:ring-primary/20"
                    value={formState.toAccountId}
                    onChange={(event) =>
                      setFormState((prev) => ({ ...prev, toAccountId: event.target.value }))
                    }
                    disabled={transferMutation.isPending}
                  >
                    <option value="">Select account</option>
                    {accounts.map((account) => (
                      <option key={account.id} value={account.id}>
                        {account.accountType} - {account.accountNumber}
                      </option>
                    ))}
                  </select>
                  {errors.toAccountId ? <p className="text-xs text-danger">{errors.toAccountId}</p> : null}
                </label>

                <label className="block space-y-1 text-sm">
                  <span className="font-medium text-slate">Amount</span>
                  <Input
                    type="number"
                    min="0"
                    step="0.01"
                    value={formState.amount}
                    onChange={(event) => setFormState((prev) => ({ ...prev, amount: event.target.value }))}
                    disabled={transferMutation.isPending}
                  />
                  {errors.amount ? <p className="text-xs text-danger">{errors.amount}</p> : null}
                </label>

                <label className="block space-y-1 text-sm">
                  <span className="font-medium text-slate">Description (optional)</span>
                  <Input
                    value={formState.description}
                    onChange={(event) =>
                      setFormState((prev) => ({ ...prev, description: event.target.value }))
                    }
                    disabled={transferMutation.isPending}
                  />
                  {errors.description ? <p className="text-xs text-danger">{errors.description}</p> : null}
                </label>

                <Button type="submit" disabled={transferMutation.isPending}>
                  {transferMutation.isPending ? "Submitting transfer..." : "Submit transfer"}
                </Button>
              </form>
            )}
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <div>
              <CardTitle>Transfer status</CardTitle>
              <CardDescription>Latest request metadata for operations traceability.</CardDescription>
            </div>
          </CardHeader>
          <CardContent>
            {receipt ? (
              <div className="space-y-3 rounded-xl border border-success/20 bg-success/5 p-4">
                <p className="text-sm font-semibold text-success">Transfer submitted successfully.</p>
                <div>
                  <p className="text-xs uppercase tracking-wide text-muted">Idempotency Key</p>
                  <p className="break-all font-mono text-sm text-ink">{receipt.idempotencyKey}</p>
                </div>
                <div>
                  <p className="text-xs uppercase tracking-wide text-muted">Submitted</p>
                  <p className="text-sm text-ink">{formatDateTime(receipt.timestamp)}</p>
                </div>
              </div>
            ) : (
              <p className="text-sm text-slate">No transfer has been submitted in this session.</p>
            )}

            {selectedFrom ? (
              <div className="mt-4 rounded-xl border border-line bg-surface p-4">
                <p className="text-xs uppercase tracking-wide text-muted">Source account balance</p>
                <p className="text-xl font-semibold text-ink">
                  {formatCurrency(selectedFrom.balance, selectedFrom.currency || "ZAR")}
                </p>
              </div>
            ) : null}
          </CardContent>
        </Card>
      </div>
    </div>
  );
}



