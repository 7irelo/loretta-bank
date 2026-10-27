"use client";

import { useQuery, useQueryClient } from "@tanstack/react-query";
import { useState, type FormEvent } from "react";
import toast from "react-hot-toast";

import { Amount, EmptyState, Field, Select } from "@/components/form";
import { SectionHeader } from "@/components/section-header";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import {
  accountsApi,
  auditApi,
  customersApi,
  errorMessage,
  newIdempotencyKey,
  transactionsApi,
  usersApi
} from "@/lib/api";
import { cn, formatDateTime } from "@/lib/utils";
import { useAuthStore } from "@/store/auth-store";
import type { KycStatus } from "@/types/api";

type Tab = "kyc" | "accounts" | "cash" | "ledger" | "audit" | "users";

export default function AdminPage() {
  const role = useAuthStore((s) => s.user?.role);
  const isAdmin = role === "ADMIN";
  const [tab, setTab] = useState<Tab>("kyc");
  const tabs: { id: Tab; label: string; adminOnly?: boolean }[] = [
    { id: "kyc", label: "KYC queue" },
    { id: "accounts", label: "Accounts" },
    { id: "cash", label: "Cash deposit", adminOnly: true },
    { id: "ledger", label: "Ledger health", adminOnly: true },
    { id: "audit", label: "Audit log" },
    { id: "users", label: "Users", adminOnly: true }
  ];

  return (
    <div className="space-y-6">
      <SectionHeader title="Back office" description="Customer verification, account operations and oversight." />
      <div className="flex flex-wrap gap-1 rounded-xl border border-line bg-white p-1" role="tablist">
        {tabs.filter((t) => isAdmin || !t.adminOnly).map((t) => (
          <button key={t.id} role="tab" aria-selected={tab === t.id} onClick={() => setTab(t.id)}
            className={cn("rounded-lg px-3 py-2 text-sm font-medium", tab === t.id ? "bg-primary text-white" : "text-slate hover:text-ink")}>
            {t.label}
          </button>
        ))}
      </div>
      {tab === "kyc" ? <KycQueue canDecide={isAdmin} /> : null}
      {tab === "accounts" ? <AccountsTable canAct={isAdmin} /> : null}
      {tab === "cash" && isAdmin ? <CashDeposit /> : null}
      {tab === "ledger" && isAdmin ? <LedgerHealth /> : null}
      {tab === "audit" ? <AuditLogTable /> : null}
      {tab === "users" && isAdmin ? <Users /> : null}
    </div>
  );
}

function KycQueue({ canDecide }: { canDecide: boolean }) {
  const queryClient = useQueryClient();
  const [status, setStatus] = useState<KycStatus>("PENDING");
  const customers = useQuery({ queryKey: ["admin", "customers", status], queryFn: () => customersApi.list(0, status) });

  const decide = async (id: number, next: KycStatus) => {
    try {
      await customersApi.setKyc(id, next);
      toast.success(`Customer ${next.toLowerCase()}`);
      await queryClient.invalidateQueries({ queryKey: ["admin", "customers"] });
    } catch (err) {
      toast.error(errorMessage(err, "Update failed"));
    }
  };

  return (
    <Card>
      <CardHeader>
        <CardTitle>Identity verification</CardTitle>
        <div className="w-40"><Select value={status} onChange={(e) => setStatus(e.target.value as KycStatus)} aria-label="Status">
          <option value="PENDING">Pending</option><option value="VERIFIED">Verified</option><option value="REJECTED">Rejected</option>
        </Select></div>
      </CardHeader>
      <CardContent>
        {(customers.data?.content ?? []).length === 0 ? <EmptyState title="Nothing in this queue" /> : (
          <div className="table-shell"><table className="table-base">
            <thead><tr><th>Name</th><th>Email</th><th>ID number</th><th>Submitted</th><th /></tr></thead>
            <tbody>{customers.data?.content.map((c) => (
              <tr key={c.id}>
                <td>{c.firstName} {c.lastName}</td><td>{c.email}</td>
                <td className="font-mono">{c.idNumber ? `${c.idNumber.slice(0, 6)}*******` : "-"}</td>
                <td>{c.createdAt ? formatDateTime(c.createdAt) : "-"}</td>
                <td className="space-x-2 text-right">
                  {canDecide && c.kycStatus !== "VERIFIED" ? <Button size="sm" onClick={() => decide(c.id, "VERIFIED")}>Approve</Button> : null}
                  {canDecide && c.kycStatus !== "REJECTED" ? <Button size="sm" variant="secondary" onClick={() => decide(c.id, "REJECTED")}>Reject</Button> : null}
                </td>
              </tr>
            ))}</tbody>
          </table></div>
        )}
      </CardContent>
    </Card>
  );
}

function AccountsTable({ canAct }: { canAct: boolean }) {
  const queryClient = useQueryClient();
  const [page, setPage] = useState(0);
  const accounts = useQuery({ queryKey: ["admin", "accounts", page], queryFn: () => accountsApi.list(page) });

  const act = async (id: number, action: "freeze" | "unfreeze" | "close") => {
    try {
      await accountsApi.action(id, action);
      toast.success(`Account ${action === "close" ? "closed" : `${action}d`}`);
      await queryClient.invalidateQueries({ queryKey: ["admin", "accounts"] });
    } catch (err) {
      toast.error(errorMessage(err, "Action failed"));
    }
  };

  return (
    <Card>
      <CardContent>
        <div className="table-shell"><table className="table-base">
          <thead><tr><th>Account</th><th>Customer</th><th>Type</th><th>Status</th><th className="text-right">Balance</th><th /></tr></thead>
          <tbody>{(accounts.data?.content ?? []).map((a) => (
            <tr key={a.id}>
              <td className="font-mono">{a.accountNumber}</td><td>#{a.customerId}</td><td>{a.accountType}</td>
              <td><Badge variant={a.status === "ACTIVE" ? "success" : "danger"}>{a.status}</Badge></td>
              <td className="text-right"><Amount value={Number(a.balance)} /></td>
              <td className="space-x-2 text-right">
                {canAct && a.status === "ACTIVE" ? <Button size="sm" variant="secondary" onClick={() => act(a.id, "freeze")}>Freeze</Button> : null}
                {canAct && a.status === "FROZEN" ? <Button size="sm" variant="secondary" onClick={() => act(a.id, "unfreeze")}>Unfreeze</Button> : null}
                {canAct && a.status !== "CLOSED" && Number(a.balance) === 0 ? <Button size="sm" variant="danger" onClick={() => act(a.id, "close")}>Close</Button> : null}
              </td>
            </tr>
          ))}</tbody>
        </table></div>
        <div className="mt-4 flex justify-end gap-2">
          <Button size="sm" variant="secondary" disabled={page === 0} onClick={() => setPage((p) => p - 1)}>Previous</Button>
          <Button size="sm" variant="secondary" disabled={accounts.data?.last ?? true} onClick={() => setPage((p) => p + 1)}>Next</Button>
        </div>
      </CardContent>
    </Card>
  );
}

function CashDeposit() {
  const [accountId, setAccountId] = useState("");
  const [amount, setAmount] = useState("");
  const [note, setNote] = useState("Branch cash deposit");
  const [pending, setPending] = useState(false);

  const submit = async (event: FormEvent) => {
    event.preventDefault();
    const value = Number(amount);
    if (!accountId || !(value > 0)) {
      toast.error("Enter an account id and a positive amount");
      return;
    }
    setPending(true);
    try {
      const t = await transactionsApi.deposit(Number(accountId), value, note, newIdempotencyKey());
      toast.success(`Deposited (${t.reference})`);
      setAmount("");
    } catch (err) {
      toast.error(errorMessage(err, "Deposit failed"));
    } finally {
      setPending(false);
    }
  };

  return (
    <Card>
      <CardHeader><CardTitle>Record a cash deposit</CardTitle></CardHeader>
      <CardContent>
        <form onSubmit={submit} className="grid gap-4 md:grid-cols-4">
          <Field label="Account id"><Input inputMode="numeric" value={accountId} onChange={(e) => setAccountId(e.target.value)} /></Field>
          <Field label="Amount (R)"><Input inputMode="decimal" value={amount} onChange={(e) => setAmount(e.target.value)} /></Field>
          <Field label="Note"><Input value={note} maxLength={140} onChange={(e) => setNote(e.target.value)} /></Field>
          <div><Button type="submit" disabled={pending}>{pending ? "Recording…" : "Record deposit"}</Button></div>
        </form>
      </CardContent>
    </Card>
  );
}

function LedgerHealth() {
  const report = useQuery({ queryKey: ["admin", "reconciliation"], queryFn: accountsApi.reconciliation });
  const r = report.data;
  return (
    <Card>
      <CardHeader><CardTitle>Ledger reconciliation</CardTitle>
        <Button size="sm" variant="secondary" onClick={() => report.refetch()}>Run now</Button></CardHeader>
      <CardContent className="space-y-2 text-sm">
        {r ? (
          <>
            <p>Status: <Badge variant={r.healthy ? "success" : "danger"}>{r.healthy ? "balanced" : "drift detected"}</Badge></p>
            <p className="text-slate">Sum of all postings: {r.ledgerTotal} (must be 0)</p>
            <p className="text-slate">Accounts whose balance differs from their postings: {r.accountsWithDrift.length ? r.accountsWithDrift.join(", ") : "none"}</p>
            <p className="text-xs text-muted">Checked {formatDateTime(r.checkedAt)}</p>
          </>
        ) : <p className="text-slate">Loading…</p>}
      </CardContent>
    </Card>
  );
}

function AuditLogTable() {
  const [page, setPage] = useState(0);
  const [eventType, setEventType] = useState("");
  const audit = useQuery({ queryKey: ["admin", "audit", page, eventType], queryFn: () => auditApi.search(page, eventType) });
  return (
    <Card>
      <CardHeader>
        <CardTitle>Audit log</CardTitle>
        <div className="w-56"><Select value={eventType} onChange={(e) => { setEventType(e.target.value); setPage(0); }} aria-label="Event type">
          <option value="">All events</option>
          {["CUSTOMER_CREATED", "ACCOUNT_OPENED", "MONEY_DEPOSITED", "MONEY_WITHDRAWN", "TRANSFER_COMPLETED", "TRANSFER_FAILED"].map((t) => <option key={t} value={t}>{t}</option>)}
        </Select></div>
      </CardHeader>
      <CardContent>
        <div className="table-shell"><table className="table-base">
          <thead><tr><th>When</th><th>Event</th><th>Aggregate</th><th>Event id</th></tr></thead>
          <tbody>{(audit.data?.content ?? []).map((e) => (
            <tr key={e.id}><td>{formatDateTime(e.occurredAt)}</td><td>{e.eventType}</td><td>{e.aggregateType} #{e.aggregateId}</td><td className="font-mono text-xs">{e.eventId}</td></tr>
          ))}</tbody>
        </table></div>
        <div className="mt-4 flex justify-end gap-2">
          <Button size="sm" variant="secondary" disabled={page === 0} onClick={() => setPage((p) => p - 1)}>Newer</Button>
          <Button size="sm" variant="secondary" disabled={audit.data?.last ?? true} onClick={() => setPage((p) => p + 1)}>Older</Button>
        </div>
      </CardContent>
    </Card>
  );
}

function Users() {
  const queryClient = useQueryClient();
  const self = useAuthStore((s) => s.user?.id);
  const users = useQuery({ queryKey: ["admin", "users"], queryFn: usersApi.list });
  const setRole = async (id: number, role: string) => {
    try {
      await usersApi.setRole(id, role);
      toast.success("Role updated");
      await queryClient.invalidateQueries({ queryKey: ["admin", "users"] });
    } catch (err) {
      toast.error(errorMessage(err, "Could not change the role"));
    }
  };
  return (
    <Card>
      <CardContent>
        <div className="table-shell"><table className="table-base">
          <thead><tr><th>Name</th><th>Email</th><th>Role</th></tr></thead>
          <tbody>{(users.data ?? []).map((u) => (
            <tr key={u.id}>
              <td>{u.firstName} {u.lastName}</td><td>{u.email}</td>
              <td className="w-44">
                <Select value={u.role} disabled={String(u.id) === self} aria-label={`Role for ${u.email}`}
                  onChange={(e) => setRole(u.id, e.target.value)}>
                  <option value="CUSTOMER">Customer</option><option value="SUPPORT">Support</option><option value="ADMIN">Admin</option>
                </Select>
              </td>
            </tr>
          ))}</tbody>
        </table></div>
      </CardContent>
    </Card>
  );
}
