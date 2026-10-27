"use client";

import { useState, type FormEvent } from "react";

import { Field } from "@/components/form";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { authApi, errorMessage } from "@/lib/api";

/** Asks for the password again before a sensitive action (paying a new account). */
export function StepUpDialog({
  open,
  onConfirmed,
  onCancel
}: {
  open: boolean;
  onConfirmed: () => void;
  onCancel: () => void;
}) {
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [pending, setPending] = useState(false);

  if (!open) {
    return null;
  }

  const submit = async (event: FormEvent) => {
    event.preventDefault();
    setPending(true);
    setError(null);
    try {
      await authApi.stepUp(password);
      setPassword("");
      onConfirmed();
    } catch (err) {
      setError(errorMessage(err, "Password is incorrect"));
    } finally {
      setPending(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 grid place-items-center bg-ink/40 p-4" role="dialog" aria-modal="true"
      aria-labelledby="step-up-title">
      <form onSubmit={submit} className="w-full max-w-sm space-y-4 rounded-2xl bg-white p-6 shadow-soft">
        <div>
          <h2 id="step-up-title" className="font-display text-lg font-semibold text-ink">Confirm it&apos;s you</h2>
          <p className="text-sm text-slate">Enter your password to approve this payment.</p>
        </div>
        <Field label="Password" error={error ?? undefined}>
          <Input type="password" autoComplete="current-password" autoFocus value={password}
            onChange={(e) => setPassword(e.target.value)} disabled={pending} />
        </Field>
        <div className="flex justify-end gap-2">
          <Button variant="secondary" onClick={onCancel} disabled={pending}>Cancel</Button>
          <Button type="submit" disabled={pending || !password}>{pending ? "Checking…" : "Confirm"}</Button>
        </div>
      </form>
    </div>
  );
}
