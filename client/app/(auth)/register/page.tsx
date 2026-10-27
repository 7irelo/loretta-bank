"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState, type FormEvent } from "react";
import { z } from "zod";

import { Field } from "@/components/form";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { authApi, errorMessage } from "@/lib/api";
import { useAuthStore } from "@/store/auth-store";

const schema = z.object({
  fullName: z.string().trim().min(2, "Enter your full name").max(200),
  email: z.string().trim().email("Enter a valid email"),
  password: z
    .string()
    .min(12, "At least 12 characters")
    .max(128)
    .regex(/[a-z]/, "Add a lowercase letter")
    .regex(/[A-Z]/, "Add an uppercase letter")
    .regex(/\d/, "Add a number")
    .regex(/[^A-Za-z0-9]/, "Add a symbol")
});

type Form = z.infer<typeof schema>;

export default function RegisterPage() {
  const router = useRouter();
  const setUser = useAuthStore((s) => s.setUser);
  const [form, setForm] = useState<Form>({ fullName: "", email: "", password: "" });
  const [errors, setErrors] = useState<Partial<Record<keyof Form, string>>>({});
  const [error, setError] = useState<string | null>(null);
  const [pending, setPending] = useState(false);

  const submit = async (event: FormEvent) => {
    event.preventDefault();
    const parsed = schema.safeParse(form);
    if (!parsed.success) {
      const f = parsed.error.flatten().fieldErrors;
      setErrors({ fullName: f.fullName?.[0], email: f.email?.[0], password: f.password?.[0] });
      return;
    }
    setErrors({});
    setPending(true);
    setError(null);
    try {
      const { user } = await authApi.register(parsed.data.fullName, parsed.data.email, parsed.data.password);
      setUser(user);
      router.replace("/onboarding");
    } catch (err) {
      setError(errorMessage(err, "Unable to create your account."));
    } finally {
      setPending(false);
    }
  };

  const set = (key: keyof Form) => (e: React.ChangeEvent<HTMLInputElement>) =>
    setForm((prev) => ({ ...prev, [key]: e.target.value }));

  return (
    <Card>
      <CardHeader>
        <div>
          <CardTitle>Open your account</CardTitle>
          <CardDescription>Takes about three minutes. You&apos;ll need your SA ID number.</CardDescription>
        </div>
      </CardHeader>
      <CardContent>
        <form className="space-y-4" onSubmit={submit} noValidate>
          <Field label="Full name" error={errors.fullName}>
            <Input autoComplete="name" value={form.fullName} onChange={set("fullName")} disabled={pending} />
          </Field>
          <Field label="Email" error={errors.email}>
            <Input type="email" autoComplete="email" value={form.email} onChange={set("email")} disabled={pending} />
          </Field>
          <Field label="Password" error={errors.password}
            hint="12+ characters with upper and lower case letters, a number and a symbol.">
            <Input type="password" autoComplete="new-password" value={form.password}
              onChange={set("password")} disabled={pending} />
          </Field>
          {error ? <p className="text-sm text-danger" role="alert">{error}</p> : null}
          <Button type="submit" className="w-full" disabled={pending}>
            {pending ? "Creating account…" : "Continue"}
          </Button>
          <p className="text-center text-sm text-slate">
            Already bank with us? <Link href="/login" className="font-semibold text-primary">Sign in</Link>
          </p>
        </form>
      </CardContent>
    </Card>
  );
}
