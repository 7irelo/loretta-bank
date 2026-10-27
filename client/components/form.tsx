import type { ReactNode, SelectHTMLAttributes } from "react";

import { cn } from "@/lib/utils";

export function Field({
  label,
  error,
  hint,
  children
}: {
  label: string;
  error?: string;
  hint?: string;
  children: ReactNode;
}) {
  return (
    <label className="block space-y-1 text-sm">
      <span className="font-medium text-slate">{label}</span>
      {children}
      {error ? (
        <p className="text-xs text-danger" role="alert">
          {error}
        </p>
      ) : hint ? (
        <p className="text-xs text-muted">{hint}</p>
      ) : null}
    </label>
  );
}

export function Select({ className, ...props }: SelectHTMLAttributes<HTMLSelectElement>) {
  return (
    <select
      className={cn(
        "h-10 w-full rounded-xl border border-line bg-white px-3 text-sm text-ink outline-none focus:border-primary focus:ring-4 focus:ring-primary/20 disabled:opacity-60",
        className
      )}
      {...props}
    />
  );
}

export function EmptyState({ title, children }: { title: string; children?: ReactNode }) {
  return (
    <div className="rounded-2xl border border-dashed border-line p-8 text-center">
      <p className="font-medium text-ink">{title}</p>
      {children ? <div className="mt-2 text-sm text-slate">{children}</div> : null}
    </div>
  );
}

export function Amount({
  value,
  direction,
  currency = "ZAR"
}: {
  value: number;
  direction?: "in" | "out";
  currency?: string;
}) {
  const formatted = new Intl.NumberFormat("en-ZA", { style: "currency", currency }).format(
    Math.abs(value)
  );
  return (
    <span
      className={cn(
        "font-semibold tabular-nums",
        direction === "in" ? "text-success" : direction === "out" ? "text-ink" : "text-ink"
      )}
    >
      {direction === "in" ? "+" : direction === "out" ? "−" : ""}
      {formatted}
    </span>
  );
}
