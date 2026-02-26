"use client";

import { useMemo, useState, type FormEvent } from "react";
import { z } from "zod";

import { useCreateAccountMutation, useCustomersQuery } from "@/lib/hooks";
import { useAuthStore } from "@/store/auth-store";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { cn } from "@/lib/utils";

const createAccountSchema = z.object({
  customerId: z.string().min(1, "Customer is required."),
  accountType: z.string().min(1, "Account type is required."),
  currency: z.string().min(3, "Currency is required.").max(3, "Currency must be 3 letters."),
  initialDeposit: z.coerce.number().min(0, "Initial deposit cannot be negative.")
});

interface CreateAccountFormProps {
  className?: string;
}

const accountTypes = ["CHECKING", "SAVINGS"];

export function CreateAccountForm({ className }: CreateAccountFormProps) {
  const user = useAuthStore((state) => state.user);
  const isAdmin = user?.role === "ADMIN";
  const { data: customers = [], isLoading: customersLoading } = useCustomersQuery(isAdmin);
  const createAccountMutation = useCreateAccountMutation();

  const [formState, setFormState] = useState({
    customerId: "",
    accountType: "CHECKING",
    currency: "ZAR",
    initialDeposit: "0"
  });
  const [errors, setErrors] = useState<Partial<Record<keyof typeof formState, string>>>({});

  const customerOptions = useMemo(() => {
    return customers.map((customer) => {
      const displayName = `${customer.firstName || ""} ${customer.lastName || ""}`.trim() || customer.email;

      return {
        id: customer.id,
        label: `${displayName} (${customer.email})`
      };
    });
  }, [customers]);

  if (!isAdmin) {
    return null;
  }

  const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    const parsed = createAccountSchema.safeParse({
      customerId: formState.customerId,
      accountType: formState.accountType,
      currency: formState.currency.toUpperCase(),
      initialDeposit: formState.initialDeposit
    });

    if (!parsed.success) {
      const fieldErrors = parsed.error.flatten().fieldErrors;

      setErrors({
        customerId: fieldErrors.customerId?.[0],
        accountType: fieldErrors.accountType?.[0],
        currency: fieldErrors.currency?.[0],
        initialDeposit: fieldErrors.initialDeposit?.[0]
      });
      return;
    }

    setErrors({});

    createAccountMutation.mutate(parsed.data, {
      onSuccess: () => {
        setFormState({
          customerId: "",
          accountType: "CHECKING",
          currency: "ZAR",
          initialDeposit: "0"
        });
      }
    });
  };

  return (
    <Card className={cn(className)}>
      <CardHeader>
        <div>
          <CardTitle>Create Customer Account</CardTitle>
          <CardDescription>Admin-only account onboarding for retail and business customers.</CardDescription>
        </div>
      </CardHeader>
      <CardContent>
        <form className="grid gap-4 md:grid-cols-2" onSubmit={handleSubmit}>
          <label className="space-y-1 text-sm md:col-span-2">
            <span className="font-medium text-slate">Customer</span>
            <select
              className="h-10 w-full rounded-xl border border-line bg-white px-3 text-sm text-ink outline-none focus:border-primary focus:ring-4 focus:ring-primary/20"
              value={formState.customerId}
              onChange={(event) => setFormState((prev) => ({ ...prev, customerId: event.target.value }))}
              disabled={customersLoading || createAccountMutation.isPending}
            >
              <option value="">Select a customer</option>
              {customerOptions.map((customer) => (
                <option key={customer.id} value={customer.id}>
                  {customer.label}
                </option>
              ))}
            </select>
            {errors.customerId ? <p className="text-xs text-danger">{errors.customerId}</p> : null}
          </label>

          <label className="space-y-1 text-sm">
            <span className="font-medium text-slate">Account Type</span>
            <select
              className="h-10 w-full rounded-xl border border-line bg-white px-3 text-sm text-ink outline-none focus:border-primary focus:ring-4 focus:ring-primary/20"
              value={formState.accountType}
              onChange={(event) => setFormState((prev) => ({ ...prev, accountType: event.target.value }))}
              disabled={createAccountMutation.isPending}
            >
              {accountTypes.map((accountType) => (
                <option key={accountType} value={accountType}>
                  {accountType}
                </option>
              ))}
            </select>
            {errors.accountType ? <p className="text-xs text-danger">{errors.accountType}</p> : null}
          </label>

          <label className="space-y-1 text-sm">
            <span className="font-medium text-slate">Currency</span>
            <Input
              maxLength={3}
              value={formState.currency}
              onChange={(event) =>
                setFormState((prev) => ({ ...prev, currency: event.target.value.toUpperCase() }))
              }
              disabled={createAccountMutation.isPending}
            />
            {errors.currency ? <p className="text-xs text-danger">{errors.currency}</p> : null}
          </label>

          <label className="space-y-1 text-sm md:col-span-2">
            <span className="font-medium text-slate">Initial Deposit</span>
            <Input
              type="number"
              min="0"
              step="0.01"
              value={formState.initialDeposit}
              onChange={(event) => setFormState((prev) => ({ ...prev, initialDeposit: event.target.value }))}
              disabled={createAccountMutation.isPending}
            />
            {errors.initialDeposit ? <p className="text-xs text-danger">{errors.initialDeposit}</p> : null}
          </label>

          <div className="md:col-span-2">
            <Button type="submit" disabled={createAccountMutation.isPending || customersLoading}>
              {createAccountMutation.isPending ? "Creating account..." : "Create account"}
            </Button>
          </div>
        </form>
      </CardContent>
    </Card>
  );
}


