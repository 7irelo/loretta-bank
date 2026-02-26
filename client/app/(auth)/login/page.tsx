"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useState, type FormEvent } from "react";
import { z } from "zod";

import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { useLoginMutation } from "@/lib/hooks";
import { useAuthStore } from "@/store/auth-store";

const loginSchema = z.object({
  email: z.string().email("Enter a valid email address."),
  password: z.string().min(8, "Password must be at least 8 characters.")
});

type LoginFormState = z.infer<typeof loginSchema>;

export default function LoginPage() {
  const router = useRouter();
  const loginMutation = useLoginMutation();
  const token = useAuthStore((state) => state.token);
  const hydrated = useAuthStore((state) => state.hydrated);

  const [formState, setFormState] = useState<LoginFormState>({
    email: "",
    password: ""
  });
  const [errors, setErrors] = useState<Partial<Record<keyof LoginFormState, string>>>({});

  useEffect(() => {
    if (hydrated && token) {
      router.replace("/dashboard");
    }
  }, [hydrated, router, token]);

  const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    const parsed = loginSchema.safeParse(formState);

    if (!parsed.success) {
      const fieldErrors = parsed.error.flatten().fieldErrors;

      setErrors({
        email: fieldErrors.email?.[0],
        password: fieldErrors.password?.[0]
      });
      return;
    }

    setErrors({});

    loginMutation.mutate(parsed.data, {
      onSuccess: () => {
        router.replace("/dashboard");
      }
    });
  };

  return (
    <Card>
      <CardHeader>
        <div>
          <CardTitle>Welcome back</CardTitle>
          <CardDescription>Sign in to continue to your banking workspace.</CardDescription>
        </div>
      </CardHeader>
      <CardContent>
        <form className="space-y-4" onSubmit={handleSubmit}>
          <label className="block space-y-1 text-sm">
            <span className="font-medium text-slate">Email</span>
            <Input
              type="email"
              value={formState.email}
              onChange={(event) => setFormState((prev) => ({ ...prev, email: event.target.value }))}
              autoComplete="email"
              disabled={loginMutation.isPending}
            />
            {errors.email ? <p className="text-xs text-danger">{errors.email}</p> : null}
          </label>

          <label className="block space-y-1 text-sm">
            <span className="font-medium text-slate">Password</span>
            <Input
              type="password"
              value={formState.password}
              onChange={(event) => setFormState((prev) => ({ ...prev, password: event.target.value }))}
              autoComplete="current-password"
              disabled={loginMutation.isPending}
            />
            {errors.password ? <p className="text-xs text-danger">{errors.password}</p> : null}
          </label>

          <Button type="submit" className="w-full" disabled={loginMutation.isPending}>
            {loginMutation.isPending ? "Signing in..." : "Sign in"}
          </Button>
        </form>

        <p className="mt-4 text-sm text-slate">
          Need an account?{" "}
          <Link href="/register" className="font-semibold text-primary">
            Register
          </Link>
        </p>
      </CardContent>
    </Card>
  );
}


