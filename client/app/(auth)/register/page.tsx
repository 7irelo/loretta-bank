"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useState, type FormEvent } from "react";
import { z } from "zod";

import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { useRegisterMutation } from "@/lib/hooks";
import { useAuthStore } from "@/store/auth-store";

const registerSchema = z
  .object({
    fullName: z.string().min(2, "Full name is required."),
    email: z.string().email("Enter a valid email address."),
    password: z
      .string()
      .min(8, "Password must be at least 8 characters.")
      .regex(/[A-Z]/, "Password must include an uppercase letter.")
      .regex(/[0-9]/, "Password must include a number."),
    confirmPassword: z.string()
  })
  .refine((values) => values.password === values.confirmPassword, {
    path: ["confirmPassword"],
    message: "Passwords do not match."
  });

type RegisterFormState = z.infer<typeof registerSchema>;

export default function RegisterPage() {
  const router = useRouter();
  const registerMutation = useRegisterMutation();
  const token = useAuthStore((state) => state.token);
  const hydrated = useAuthStore((state) => state.hydrated);

  const [formState, setFormState] = useState<RegisterFormState>({
    fullName: "",
    email: "",
    password: "",
    confirmPassword: ""
  });
  const [errors, setErrors] = useState<Partial<Record<keyof RegisterFormState, string>>>({});

  useEffect(() => {
    if (hydrated && token) {
      router.replace("/dashboard");
    }
  }, [hydrated, router, token]);

  const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    const parsed = registerSchema.safeParse(formState);

    if (!parsed.success) {
      const fieldErrors = parsed.error.flatten().fieldErrors;

      setErrors({
        fullName: fieldErrors.fullName?.[0],
        email: fieldErrors.email?.[0],
        password: fieldErrors.password?.[0],
        confirmPassword: fieldErrors.confirmPassword?.[0]
      });
      return;
    }

    setErrors({});

    registerMutation.mutate(
      {
        fullName: parsed.data.fullName,
        email: parsed.data.email,
        password: parsed.data.password
      },
      {
        onSuccess: () => {
          router.replace("/dashboard");
        }
      }
    );
  };

  return (
    <Card>
      <CardHeader>
        <div>
          <CardTitle>Create your profile</CardTitle>
          <CardDescription>Register to access Loretta Bank digital services.</CardDescription>
        </div>
      </CardHeader>
      <CardContent>
        <form className="space-y-4" onSubmit={handleSubmit}>
          <label className="block space-y-1 text-sm">
            <span className="font-medium text-slate">Full name</span>
            <Input
              value={formState.fullName}
              onChange={(event) => setFormState((prev) => ({ ...prev, fullName: event.target.value }))}
              autoComplete="name"
              disabled={registerMutation.isPending}
            />
            {errors.fullName ? <p className="text-xs text-danger">{errors.fullName}</p> : null}
          </label>

          <label className="block space-y-1 text-sm">
            <span className="font-medium text-slate">Email</span>
            <Input
              type="email"
              value={formState.email}
              onChange={(event) => setFormState((prev) => ({ ...prev, email: event.target.value }))}
              autoComplete="email"
              disabled={registerMutation.isPending}
            />
            {errors.email ? <p className="text-xs text-danger">{errors.email}</p> : null}
          </label>

          <label className="block space-y-1 text-sm">
            <span className="font-medium text-slate">Password</span>
            <Input
              type="password"
              value={formState.password}
              onChange={(event) => setFormState((prev) => ({ ...prev, password: event.target.value }))}
              autoComplete="new-password"
              disabled={registerMutation.isPending}
            />
            {errors.password ? <p className="text-xs text-danger">{errors.password}</p> : null}
          </label>

          <label className="block space-y-1 text-sm">
            <span className="font-medium text-slate">Confirm password</span>
            <Input
              type="password"
              value={formState.confirmPassword}
              onChange={(event) =>
                setFormState((prev) => ({ ...prev, confirmPassword: event.target.value }))
              }
              autoComplete="new-password"
              disabled={registerMutation.isPending}
            />
            {errors.confirmPassword ? <p className="text-xs text-danger">{errors.confirmPassword}</p> : null}
          </label>

          <Button type="submit" className="w-full" disabled={registerMutation.isPending}>
            {registerMutation.isPending ? "Creating account..." : "Create account"}
          </Button>
        </form>

        <p className="mt-4 text-sm text-slate">
          Already registered?{" "}
          <Link href="/login" className="font-semibold text-primary">
            Sign in
          </Link>
        </p>
      </CardContent>
    </Card>
  );
}


