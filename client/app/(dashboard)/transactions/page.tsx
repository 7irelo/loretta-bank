"use client";

import { useMemo, useState, type FormEvent } from "react";
import { Sparkles } from "lucide-react";

import { Field, Select } from "@/components/form";
import { SectionHeader } from "@/components/section-header";
import { TransactionList } from "@/components/transaction-list";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Skeleton } from "@/components/ui/skeleton";
import { aiApi, errorMessage, type TransactionQuery } from "@/lib/api";
import { useMyAccounts, useMyTransactions } from "@/lib/hooks";
import type { SearchResult, TransactionType } from "@/types/api";

const toInstant = (day: string, endOfDay = false) =>
  day ? new Date(`${day}T${endOfDay ? "23:59:59.999" : "00:00:00"}+02:00`).toISOString() : undefined;

export default function TransactionsPage() {
  const accounts = useMyAccounts();
  const ids = useMemo(() => new Set((accounts.data ?? []).map((a) => a.id)), [accounts.data]);
  const [page, setPage] = useState(0);
  const [type, setType] = useState<TransactionType | "">("");
  const [from, setFrom] = useState("");
  const [to, setTo] = useState("");
  const [q, setQ] = useState("");
  const query: TransactionQuery = { page, size: 20, type, from: toInstant(from), to: toInstant(to, true), q };
  const transactions = useMyTransactions(query);

  const [question, setQuestion] = useState("");
  const [answer, setAnswer] = useState<SearchResult | null>(null);
  const [asking, setAsking] = useState(false);
  const [askError, setAskError] = useState<string | null>(null);

  const ask = async (event: FormEvent) => {
    event.preventDefault();
    if (question.trim().length < 2) {
      return;
    }
    setAsking(true);
    setAskError(null);
    try {
      setAnswer(await aiApi.search(question.trim()));
    } catch (err) {
      setAskError(errorMessage(err, "Search is unavailable right now."));
    } finally {
      setAsking(false);
    }
  };

  const data = transactions.data;

  return (
    <div className="space-y-6">
      <SectionHeader title="Transactions" description="Everything in and out of your accounts." />

      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2 text-base"><Sparkles className="h-4 w-4 text-primary" /> Ask about your spending</CardTitle>
        </CardHeader>
        <CardContent className="space-y-3">
          <form onSubmit={ask} className="flex gap-2">
            <Input value={question} onChange={(e) => setQuestion(e.target.value)} maxLength={300}
              placeholder="How much did I spend on Uber last month?" aria-label="Question" />
            <Button type="submit" disabled={asking}>{asking ? "Asking…" : "Ask"}</Button>
          </form>
          {askError ? <p className="text-sm text-danger">{askError}</p> : null}
          {answer ? (
            <div className="rounded-xl bg-surface p-3">
              <p className="font-medium text-ink">{answer.answer}</p>
              {answer.transactions.length ? (
                <TransactionList transactions={answer.transactions.slice(0, 10)} myAccountIds={ids} />
              ) : null}
            </div>
          ) : null}
        </CardContent>
      </Card>

      <Card>
        <CardContent className="grid gap-4 md:grid-cols-5">
          <Field label="Type">
            <Select value={type} onChange={(e) => { setType(e.target.value as TransactionType | ""); setPage(0); }}>
              <option value="">All</option>
              <option value="TRANSFER">Transfers</option>
              <option value="DEPOSIT">Deposits</option>
              <option value="WITHDRAWAL">Withdrawals</option>
            </Select>
          </Field>
          <Field label="From"><Input type="date" value={from} onChange={(e) => { setFrom(e.target.value); setPage(0); }} /></Field>
          <Field label="To"><Input type="date" value={to} onChange={(e) => { setTo(e.target.value); setPage(0); }} /></Field>
          <Field label="Search"><Input value={q} maxLength={100} onChange={(e) => { setQ(e.target.value); setPage(0); }} placeholder="Reference or description" /></Field>
          <div className="flex items-end">
            <Button variant="secondary" className="w-full" onClick={() => { setType(""); setFrom(""); setTo(""); setQ(""); setPage(0); }}>Reset</Button>
          </div>
        </CardContent>
      </Card>

      <Card>
        <CardContent>
          {transactions.isLoading ? <Skeleton className="h-60 w-full" /> : (
            <TransactionList transactions={data?.content ?? []} myAccountIds={ids} />
          )}
          <div className="mt-4 flex items-center justify-between text-xs text-slate">
            <span>Page {page + 1} of {Math.max(data?.totalPages ?? 1, 1)} · {data?.totalElements ?? 0} transactions</span>
            <div className="flex gap-2">
              <Button size="sm" variant="secondary" disabled={page === 0} onClick={() => setPage((p) => p - 1)}>Previous</Button>
              <Button size="sm" variant="secondary" disabled={data?.last ?? true} onClick={() => setPage((p) => p + 1)}>Next</Button>
            </div>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
