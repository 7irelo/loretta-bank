import { forwardRef, type InputHTMLAttributes } from "react";

import { cn } from "@/lib/utils";

export interface InputProps extends InputHTMLAttributes<HTMLInputElement> {}

export const Input = forwardRef<HTMLInputElement, InputProps>(({ className, ...props }, ref) => {
  return (
    <input
      ref={ref}
      className={cn(
        "h-10 w-full rounded-xl border border-line bg-white px-3 text-sm text-ink shadow-sm outline-none placeholder:text-muted focus:border-primary focus:ring-4 focus:ring-primary/20",
        className
      )}
      {...props}
    />
  );
});

Input.displayName = "Input";


