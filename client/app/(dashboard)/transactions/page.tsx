"use client";

import { useMemo, useState } from "react";

import { SectionHeader } from "@/components/section-header";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { useTransactionsQuery } from "@/lib/hooks";
import { formatCurrency, formatDateTime } from "@/lib/utils";

const PAGE_SIZE = 10;

export default function TransactionsPage() {
  const [page, setPage] = useState(0);
  const [type, setType] = useState("");
  const [startDate, setStartDate] = useState("");
  const [endDate, setEndDate] = useState("");

  const filters = useMemo(
    () => ({
      page,
      size: PAGE_SIZE,
      type,
      startDate,
      endDate
    }),
    [endDate, page, startDate, type]
  );

  const transactionsQuery = useTransactionsQuery(filters);
  const pageData = transactionsQuery.data;
  const rows = pageData?.content ?? [];
  const totalPages = pageData?.totalPages ?? 1;

  const applyFilterChange = (updater: () => void) => {
    updater();
    setPage(0);
  };

  return (
    <div>
      <SectionHeader
        title="Transactions"
        description="Paginated transaction history with filter controls for operations review."
      />

      <Card className="mb-6">
        <CardHeader>
          <CardTitle className="text-base">Filters</CardTitle>
        </CardHeader>
        <CardContent className="grid gap-4 md:grid-cols-4">
          <label className="space-y-1 text-sm">
            <span className="font-medium text-slate">Type</span>
            <select
              value={type}
              onChange={(event) => applyFilterChange(() => setType(event.target.value))}
              className="h-10 w-full rounded-xl border border-line bg-white px-3 text-sm text-ink outline-none focus:border-primary focus:ring-4 focus:ring-primary/20"
            >
              <option value="">All</option>
              <option value="TRANSFER">TRANSFER</option>
              <option value="DEBIT">DEBIT</option>
              <option value="CREDIT">CREDIT</option>
              <option value="PAYMENT">PAYMENT</option>
              <option value="DEPOSIT">DEPOSIT</option>
              <option value="WITHDRAWAL">WITHDRAWAL</option>
            </select>
          </label>

          <label className="space-y-1 text-sm">
            <span className="font-medium text-slate">Start date</span>
            <input
              type="date"
              value={startDate}
              onChange={(event) => applyFilterChange(() => setStartDate(event.target.value))}
              className="h-10 w-full rounded-xl border border-line bg-white px-3 text-sm text-ink outline-none focus:border-primary focus:ring-4 focus:ring-primary/20"
            />
          </label>

          <label className="space-y-1 text-sm">
            <span className="font-medium text-slate">End date</span>
            <input
              type="date"
              value={endDate}
              onChange={(event) => applyFilterChange(() => setEndDate(event.target.value))}
              className="h-10 w-full rounded-xl border border-line bg-white px-3 text-sm text-ink outline-none focus:border-primary focus:ring-4 focus:ring-primary/20"
            />
          </label>

          <div className="flex items-end">
            <Button
              variant="secondary"
              className="w-full"
              onClick={() => {
                setType("");
                setStartDate("");
                setEndDate("");
                setPage(0);
              }}
            >
              Reset
            </Button>
          </div>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle className="text-base">History</CardTitle>
        </CardHeader>
        <CardContent>
          {transactionsQuery.isLoading ? (
            <div className="space-y-3">
              <Skeleton className="h-12 w-full" />
              <Skeleton className="h-12 w-full" />
              <Skeleton className="h-12 w-full" />
            </div>
          ) : rows.length === 0 ? (
            <p className="text-sm text-slate">No transactions matched the selected filters.</p>
          ) : (
            <div className="table-shell">
              <table className="table-base">
                <thead>
                  <tr>
                    <th>Date</th>
                    <th>Type</th>
                    <th>Reference</th>
                    <th>Description</th>
                    <th className="text-right">Amount</th>
                  </tr>
                </thead>
                <tbody>
                  {rows.map((transaction) => (
                    <tr key={transaction.id}>
                      <td>{formatDateTime(transaction.createdAt)}</td>
                      <td>{transaction.type}</td>
                      <td>{transaction.reference || "-"}</td>
                      <td>{transaction.description || "-"}</td>
                      <td className="text-right font-semibold text-ink">
                        {formatCurrency(transaction.amount, transaction.currency || "ZAR")}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}

          <div className="mt-4 flex items-center justify-between">
            <p className="text-xs text-slate">
              Page {page + 1} of {Math.max(totalPages, 1)} | {pageData?.totalElements ?? rows.length} total records
            </p>
            <div className="flex items-center gap-2">
              <Button variant="secondary" size="sm" disabled={page === 0} onClick={() => setPage((prev) => prev - 1)}>
                Previous
              </Button>
              <Button
                variant="secondary"
                size="sm"
                disabled={page + 1 >= totalPages}
                onClick={() => setPage((prev) => prev + 1)}
              >
                Next
              </Button>
            </div>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}

