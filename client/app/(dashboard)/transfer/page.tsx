"use client";

import { useQueryClient } from "@tanstack/react-query";
import { useMemo, useRef, useState, type FormEvent } from "react";
import { z } from "zod";

import { Amount, Field, Select } from "@/components/form";
import { SectionHeader } from "@/components/section-header";
import { StepUpDialog } from "@/components/step-up-dialog";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { errorMessage, isStepUpRequired, newIdempotencyKey, transactionsApi } from "@/lib/api";
import { useMyAccounts } from "@/lib/hooks";
import { cn } from "@/lib/utils";
import type { Transfer } from "@/types/api";

type Mode = "own" | "other";

const schema = z.object({
  from: z.coerce.number().int().positive("Choose an account to pay from"),
  to: z.string().min(1, "Choose where the money goes"),
  amount: z.coerce
    .number()
    .positive("Enter an amount")
    .max(50000, "The per-payment limit is R50 000")
    .refine((v) => Math.round(v * 100) === v * 100, "Use at most two decimals"),
  description: z.string().trim().max(140).optional()
});

export default function TransferPage() {
  const queryClient = useQueryClient();
  const accounts = useMyAccounts();
  const list = useMemo(() => (accounts.data ?? []).filter((a) => a.status === "ACTIVE"), [accounts.data]);
  const [mode, setMode] = useState<Mode>("own");
  const [form, setForm] = useState({ from: "", to: "", amount: "", description: "" });
  const [errors, setErrors] = useState<Record<string, string | undefined>>({});
  const [review, setReview] = useState<z.infer<typeof schema> | null>(null);
  const [stepUp, setStepUp] = useState(false);
  const [pending, setPending] = useState(false);
  const [result, setResult] = useState<Transfer | null>(null);
  const [error, setError] = useState<string | null>(null);
  // One key per reviewed payment: retries after a timeout can never pay twice.
  const keyRef = useRef<string>("");

  const set = (key: keyof typeof form) => (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) =>
    setForm((prev) => ({ ...prev, [key]: e.target.value }));

  const toReview = (event: FormEvent) => {
    event.preventDefault();
    const parsed = schema.safeParse(form);
    const fieldErrors: Record<string, string | undefined> = parsed.success ? {} : Object.fromEntries(
      Object.entries(parsed.error.flatten().fieldErrors).map(([k, v]) => [k, v?.[0]])
    );
    if (parsed.success && mode === "other" && !/^LOR\d{10}$/.test(parsed.data.to.trim().toUpperCase())) {
      fieldErrors.to = "Account numbers look like LOR0123456789";
    }
    if (parsed.success && mode === "own" && parsed.data.to === String(parsed.data.from)) {
      fieldErrors.to = "Choose a different account";
    }
    setErrors(fieldErrors);
    if (!parsed.success || Object.values(fieldErrors).some(Boolean)) {
      return;
    }
    keyRef.current = newIdempotencyKey();
    setResult(null);
    setError(null);
    setReview({ ...parsed.data, to: parsed.data.to.trim().toUpperCase() });
  };

  const send = async () => {
    if (!review) {
      return;
    }
    setPending(true);
    setError(null);
    try {
      const transfer = await transactionsApi.transfer(
        {
          sourceAccountId: review.from,
          ...(mode === "own" ? { targetAccountId: Number(review.to) } : { targetAccountNumber: review.to }),
          amount: review.amount,
          description: review.description || undefined
        },
        keyRef.current
      );
      setResult(transfer);
      setReview(null);
      setForm({ from: "", to: "", amount: "", description: "" });
      await queryClient.invalidateQueries({ queryKey: ["accounts"] });
      await queryClient.invalidateQueries({ queryKey: ["transactions"] });
    } catch (err) {
      if (isStepUpRequired(err)) {
        setStepUp(true);
      } else {
        setError(errorMessage(err, "The payment could not be completed."));
      }
    } finally {
      setPending(false);
    }
  };

  const fromAccount = list.find((a) => String(a.id) === String(review?.from ?? form.from));
  const toLabel = review
    ? mode === "own"
      ? list.find((a) => String(a.id) === review.to)?.accountNumber
      : review.to
    : "";

  return (
    <div className="space-y-6">
      <SectionHeader title="Pay & transfer" description="Move money between your accounts or pay someone." />
      <div className="inline-flex rounded-xl border border-line bg-white p-1" role="tablist">
        {(["own", "other"] as Mode[]).map((m) => (
          <button key={m} role="tab" aria-selected={mode === m}
            onClick={() => { setMode(m); setForm((p) => ({ ...p, to: "" })); setReview(null); }}
            className={cn("rounded-lg px-4 py-2 text-sm font-medium",
              mode === m ? "bg-primary text-white" : "text-slate hover:text-ink")}>
            {m === "own" ? "Between my accounts" : "Pay someone"}
          </button>
        ))}
      </div>

      <div className="grid gap-6 lg:grid-cols-[1.1fr_0.9fr]">
        <Card>
          <CardContent>
            <form className="space-y-4" onSubmit={toReview} noValidate>
              <Field label="From" error={errors.from}>
                <Select value={form.from} onChange={set("from")}>
                  <option value="">Choose account</option>
                  {list.map((a) => (
                    <option key={a.id} value={a.id}>
                      {a.accountType === "CHECKING" ? "Cheque" : "Savings"} · {a.accountNumber}
                    </option>
                  ))}
                </Select>
              </Field>
              {mode === "own" ? (
                <Field label="To" error={errors.to}>
                  <Select value={form.to} onChange={set("to")}>
                    <option value="">Choose account</option>
                    {list.filter((a) => String(a.id) !== form.from).map((a) => (
                      <option key={a.id} value={a.id}>
                        {a.accountType === "CHECKING" ? "Cheque" : "Savings"} · {a.accountNumber}
                      </option>
                    ))}
                  </Select>
                </Field>
              ) : (
                <Field label="Recipient account number" error={errors.to}
                  hint="Paying a new account asks for your password.">
                  <Input value={form.to} onChange={set("to")} placeholder="LOR0123456789" autoComplete="off" />
                </Field>
              )}
              <Field label="Amount (R)" error={errors.amount}>
                <Input inputMode="decimal" value={form.amount} onChange={set("amount")} />
              </Field>
              <Field label="Reference (optional)" error={errors.description}>
                <Input value={form.description} onChange={set("description")} maxLength={140} />
              </Field>
              <Button type="submit" disabled={list.length === 0}>Review payment</Button>
            </form>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <div>
              <CardTitle>{review ? "Check and confirm" : "Summary"}</CardTitle>
              <CardDescription>Nothing is sent until you confirm.</CardDescription>
            </div>
          </CardHeader>
          <CardContent className="space-y-4 text-sm">
            {review ? (
              <>
                <dl className="space-y-2">
                  <div className="flex justify-between"><dt className="text-slate">From</dt><dd>{fromAccount?.accountNumber}</dd></div>
                  <div className="flex justify-between"><dt className="text-slate">To</dt><dd className="font-mono">{toLabel}</dd></div>
                  <div className="flex justify-between"><dt className="text-slate">Amount</dt><dd><Amount value={review.amount} /></dd></div>
                  {review.description ? (
                    <div className="flex justify-between"><dt className="text-slate">Reference</dt><dd>{review.description}</dd></div>
                  ) : null}
                </dl>
                {error ? <p className="text-danger" role="alert">{error}</p> : null}
                <div className="flex gap-2">
                  <Button onClick={send} disabled={pending}>{pending ? "Sending…" : "Confirm payment"}</Button>
                  <Button variant="secondary" onClick={() => setReview(null)} disabled={pending}>Edit</Button>
                </div>
              </>
            ) : result ? (
              <div className="rounded-xl border border-success/20 bg-success/5 p-4">
                <p className="font-semibold text-success">
                  {result.status === "PENDING" ? "Payment is being processed" : "Payment sent"}
                </p>
                <p className="text-slate">Reference {result.reference}</p>
              </div>
            ) : fromAccount ? (
              <p>Available: <Amount value={Number(fromAccount.balance)} /></p>
            ) : (
              <p className="text-slate">Fill in the form to review your payment.</p>
            )}
          </CardContent>
        </Card>
      </div>

      <StepUpDialog open={stepUp} onCancel={() => setStepUp(false)}
        onConfirmed={() => { setStepUp(false); void send(); }} />
    </div>
  );
}
