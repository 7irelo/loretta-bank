"use client";

import { useSearchParams } from "next/navigation";
import { Suspense, useState, type FormEvent } from "react";
import { Printer } from "lucide-react";

import { Amount, Field, Select } from "@/components/form";
import { SectionHeader } from "@/components/section-header";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { errorMessage, reportsApi } from "@/lib/api";
import { useMyAccounts } from "@/lib/hooks";
import { formatDateTime } from "@/lib/utils";
import type { Statement } from "@/types/api";

const firstOfMonth = () => {
  const d = new Date();
  return new Date(d.getFullYear(), d.getMonth(), 1).toISOString().slice(0, 10);
};

function Statements() {
  const params = useSearchParams();
  const accounts = useMyAccounts();
  const [accountId, setAccountId] = useState(params.get("account") ?? "");
  const [from, setFrom] = useState(firstOfMonth());
  const [to, setTo] = useState(new Date().toISOString().slice(0, 10));
  const [statement, setStatement] = useState<Statement | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [pending, setPending] = useState(false);

  const generate = async (event: FormEvent) => {
    event.preventDefault();
    if (!accountId || !from || !to) {
      setError("Choose an account and a period");
      return;
    }
    setPending(true);
    setError(null);
    try {
      setStatement(await reportsApi.generate(Number(accountId), `${from}T00:00:00`, `${to}T23:59:59`));
    } catch (err) {
      setError(errorMessage(err, "Could not generate the statement"));
    } finally {
      setPending(false);
    }
  };

  return (
    <div className="space-y-6">
      <SectionHeader title="Statements" description="Official statements for any period, ready to print or save as PDF." />
      <Card className="print:hidden">
        <CardContent>
          <form onSubmit={generate} className="grid gap-4 md:grid-cols-4">
            <Field label="Account">
              <Select value={accountId} onChange={(e) => setAccountId(e.target.value)}>
                <option value="">Choose account</option>
                {(accounts.data ?? []).map((a) => <option key={a.id} value={a.id}>{a.accountNumber}</option>)}
              </Select>
            </Field>
            <Field label="From"><Input type="date" value={from} onChange={(e) => setFrom(e.target.value)} /></Field>
            <Field label="To"><Input type="date" value={to} onChange={(e) => setTo(e.target.value)} /></Field>
            <div className="flex items-end"><Button type="submit" className="w-full" disabled={pending}>{pending ? "Generating…" : "Generate"}</Button></div>
          </form>
          {error ? <p className="mt-3 text-sm text-danger" role="alert">{error}</p> : null}
        </CardContent>
      </Card>

      {statement ? (
        <Card>
          <CardHeader>
            <div>
              <CardTitle>Loretta Bank statement · {statement.accountNumber}</CardTitle>
              <p className="text-sm text-slate">{formatDateTime(statement.periodFrom)} to {formatDateTime(statement.periodTo)}</p>
            </div>
            <Button variant="secondary" className="gap-2 print:hidden" onClick={() => window.print()}>
              <Printer className="h-4 w-4" /> Print / PDF
            </Button>
          </CardHeader>
          <CardContent className="space-y-4">
            <dl className="grid gap-3 text-sm md:grid-cols-4">
              <div><dt className="text-slate">Opening balance</dt><dd><Amount value={statement.openingBalance} /></dd></div>
              <div><dt className="text-slate">Money in</dt><dd><Amount value={statement.totalCredits} direction="in" /></dd></div>
              <div><dt className="text-slate">Money out</dt><dd><Amount value={statement.totalDebits} direction="out" /></dd></div>
              <div><dt className="text-slate">Closing balance</dt><dd><Amount value={statement.closingBalance} /></dd></div>
            </dl>
            <div className="table-shell">
              <table className="table-base">
                <thead><tr><th>Date</th><th>Description</th><th>Reference</th><th className="text-right">Amount</th></tr></thead>
                <tbody>
                  {statement.lineItems.map((line) => (
                    <tr key={line.transactionId}>
                      <td>{formatDateTime(line.createdAt)}</td>
                      <td>{line.description || line.type}</td>
                      <td>{line.reference}</td>
                      <td className="text-right"><Amount value={line.amount} direction={line.direction === "CREDIT" ? "in" : "out"} /></td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <p className="text-xs text-muted">{statement.transactionCount} transactions · generated {formatDateTime(statement.generatedAt)}</p>
          </CardContent>
        </Card>
      ) : null}
    </div>
  );
}

export default function StatementsPage() {
  return (
    <Suspense>
      <Statements />
    </Suspense>
  );
}
