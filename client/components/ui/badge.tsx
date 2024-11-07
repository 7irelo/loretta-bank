import type { HTMLAttributes } from "react";

import { cn } from "@/lib/utils";

type BadgeVariant = "default" | "soft" | "success" | "danger";

const variantClasses: Record<BadgeVariant, string> = {
  default: "bg-primary/10 text-primary",
  soft: "bg-surface text-slate",
  success: "bg-success/10 text-success",
  danger: "bg-danger/10 text-danger"
};

interface BadgeProps extends HTMLAttributes<HTMLSpanElement> {
  variant?: BadgeVariant;
}

export function Badge({ className, variant = "default", ...props }: BadgeProps) {
  return (
    <span
      className={cn("inline-flex rounded-full px-2.5 py-1 text-xs font-semibold", variantClasses[variant], className)}
      {...props}
    />
  );
}

