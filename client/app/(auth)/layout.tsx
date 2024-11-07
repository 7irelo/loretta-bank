import Link from "next/link";
import type { ReactNode } from "react";

export default function AuthLayout({ children }: { children: ReactNode }) {
  return (
    <div className="grid min-h-screen lg:grid-cols-[1.1fr_1fr]">
      <section className="relative hidden overflow-hidden bg-ink px-12 py-14 text-white lg:block">
        <div className="absolute left-[-80px] top-[-100px] h-64 w-64 rounded-full bg-accent/30 blur-2xl" />
        <div className="absolute bottom-[-120px] right-[-80px] h-72 w-72 rounded-full bg-primary/40 blur-3xl" />

        <div className="relative z-10 max-w-md space-y-6">
          <span className="inline-flex rounded-full border border-white/20 px-3 py-1 text-xs font-semibold uppercase tracking-wide">
            Loretta Bank
          </span>
          <h1 className="text-4xl font-semibold leading-tight">Bank-grade digital operations with role-aware controls.</h1>
          <p className="text-sm text-white/75">
            Sign in to monitor balances, transfer funds, review transactional history, and run admin workflows from one secure portal.
          </p>
          <div className="rounded-2xl border border-white/15 bg-white/5 p-4 text-sm text-white/80">
            Environment access is controlled by JWT-backed sessions routed through the API gateway.
          </div>
        </div>
      </section>

      <section className="grid place-items-center px-4 py-10 md:px-8">
        <div className="w-full max-w-md">
          <Link href="/" className="mb-6 inline-flex items-center gap-2 text-sm font-semibold text-primary">
            <span className="grid h-8 w-8 place-items-center rounded-lg bg-primary text-xs font-bold text-white">LB</span>
            Loretta Bank
          </Link>
          {children}
        </div>
      </section>
    </div>
  );
}


