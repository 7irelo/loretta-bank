"use client";

import { Button } from "@/components/ui/button";

export default function GlobalError({
  error,
  reset
}: {
  error: Error & { digest?: string };
  reset: () => void;
}) {
  return (
    <html>
      <body className="grid min-h-screen place-items-center bg-surface px-4">
        <div className="max-w-md rounded-2xl border border-danger/20 bg-white p-6 text-center shadow-soft">
          <h2 className="font-display text-2xl font-semibold text-ink">Something went wrong</h2>
          <p className="mt-2 text-sm text-slate">{error.message || "Unexpected application error."}</p>
          <Button className="mt-5" onClick={reset}>
            Try again
          </Button>
        </div>
      </body>
    </html>
  );
}

