"use client";

import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { Suspense, useState, type FormEvent } from "react";

import { Field } from "@/components/form";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { authApi, errorMessage } from "@/lib/api";
import { useAuthStore } from "@/store/auth-store";

function safeRedirect(target: string | null, fallback: string): string {
  // Only same-site paths; never an absolute or protocol-relative URL (open redirect).
  return target && target.startsWith("/") && !target.startsWith("//") ? target : fallback;
}

function LoginForm() {
  const router = useRouter();
  const params = useSearchParams();
  const setUser = useAuthStore((s) => s.setUser);
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [pending, setPending] = useState(false);

  const submit = async (event: FormEvent) => {
    event.preventDefault();
    setPending(true);
    setError(null);
    try {
      const { user } = await authApi.login(email.trim(), password);
      setUser(user);
      const home = user.role === "CUSTOMER" ? "/dashboard" : "/admin";
      router.replace(safeRedirect(params.get("from"), home));
    } catch (err) {
      setError(errorMessage(err, "Unable to sign in."));
    } finally {
      setPending(false);
      setPassword("");
    }
  };

  return (
    <Card>
      <CardHeader>
        <div>
          <CardTitle>Welcome back</CardTitle>
          <CardDescription>Sign in to Loretta Bank.</CardDescription>
        </div>
      </CardHeader>
      <CardContent>
        <form className="space-y-4" onSubmit={submit}>
          <Field label="Email">
            <Input type="email" autoComplete="email" required value={email}
              onChange={(e) => setEmail(e.target.value)} disabled={pending} />
          </Field>
          <Field label="Password">
            <Input type="password" autoComplete="current-password" required value={password}
              onChange={(e) => setPassword(e.target.value)} disabled={pending} />
          </Field>
          {error ? <p className="text-sm text-danger" role="alert">{error}</p> : null}
          <Button type="submit" className="w-full" disabled={pending}>
            {pending ? "Signing in…" : "Sign in"}
          </Button>
          <p className="text-center text-sm text-slate">
            New to Loretta Bank?{" "}
            <Link href="/register" className="font-semibold text-primary">Open an account</Link>
          </p>
        </form>
      </CardContent>
    </Card>
  );
}

export default function LoginPage() {
  return (
    <Suspense>
      <LoginForm />
    </Suspense>
  );
}
