import { Amount, EmptyState } from "@/components/form";
import { Badge } from "@/components/ui/badge";
import { formatDateTime } from "@/lib/utils";
import type { Transaction } from "@/types/api";

export function directionFor(t: Transaction, myAccountIds: Set<number>): "in" | "out" {
  const fromMine = t.sourceAccountId != null && myAccountIds.has(t.sourceAccountId);
  const toMine = t.targetAccountId != null && myAccountIds.has(t.targetAccountId);
  if (t.type === "DEPOSIT" || (toMine && !fromMine)) {
    return "in";
  }
  return "out";
}

const label = (t: Transaction) =>
  t.description || (t.type === "TRANSFER" ? "Transfer" : t.type === "DEPOSIT" ? "Deposit" : "Withdrawal");

export function TransactionList({
  transactions,
  myAccountIds
}: {
  transactions: Transaction[];
  myAccountIds: Set<number>;
}) {
  if (transactions.length === 0) {
    return <EmptyState title="No transactions yet" />;
  }
  return (
    <ul className="divide-y divide-line">
      {transactions.map((t) => (
        <li key={t.id} className="flex items-center justify-between gap-4 py-3">
          <div className="min-w-0">
            {/* Descriptions are written by whoever sent the money: rendered as plain text only. */}
            <p className="truncate font-medium text-ink">{label(t)}</p>
            <p className="text-xs text-slate">
              {formatDateTime(t.createdAt)} · {t.reference ?? t.type}
            </p>
          </div>
          <div className="shrink-0 text-right">
            <Amount value={t.amount} currency={t.currency} direction={directionFor(t, myAccountIds)} />
            {t.status !== "COMPLETED" ? (
              <div>
                <Badge variant={t.status === "FAILED" ? "danger" : "soft"} className="mt-1">
                  {t.status.toLowerCase()}
                </Badge>
              </div>
            ) : null}
          </div>
        </li>
      ))}
    </ul>
  );
}
