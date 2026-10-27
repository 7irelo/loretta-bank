"use client";

import { useQueryClient } from "@tanstack/react-query";
import { useEffect, useState, type FormEvent } from "react";
import toast from "react-hot-toast";

import { Field } from "@/components/form";
import { SectionHeader } from "@/components/section-header";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { customersApi, errorMessage } from "@/lib/api";
import { keys, useCustomer } from "@/lib/hooks";
import { useAuthStore } from "@/store/auth-store";

export default function SettingsPage() {
  const queryClient = useQueryClient();
  const user = useAuthStore((s) => s.user);
  const customer = useCustomer();
  const [phoneNumber, setPhone] = useState("");
  const [address, setAddress] = useState("");
  const [pending, setPending] = useState(false);

  useEffect(() => {
    setPhone(customer.data?.phoneNumber ?? "");
    setAddress(customer.data?.address ?? "");
  }, [customer.data]);

  const save = async (event: FormEvent) => {
    event.preventDefault();
    if (!customer.data) {
      return;
    }
    if (!/^(\+27|0)[1-9]\d{8}$/.test(phoneNumber.trim())) {
      toast.error("Use a South African mobile number");
      return;
    }
    setPending(true);
    try {
      await customersApi.update(customer.data.id, { phoneNumber: phoneNumber.trim(), address: address.trim() });
      await queryClient.invalidateQueries({ queryKey: keys.me });
      toast.success("Details updated");
    } catch (err) {
      toast.error(errorMessage(err, "Could not update your details"));
    } finally {
      setPending(false);
    }
  };

  const maskedId = customer.data?.idNumber ? `${customer.data.idNumber.slice(0, 6)}*******` : "-";

  return (
    <div className="max-w-3xl space-y-6">
      <SectionHeader title="Settings" description="Your profile, security and privacy." />
      <Card>
        <CardHeader><div><CardTitle>Profile</CardTitle><CardDescription>Name and ID changes need a branch visit.</CardDescription></div></CardHeader>
        <CardContent>
          <dl className="mb-4 grid gap-3 text-sm sm:grid-cols-2">
            <div><dt className="text-slate">Name</dt><dd className="text-ink">{user?.fullName}</dd></div>
            <div><dt className="text-slate">Email</dt><dd className="text-ink">{user?.email}</dd></div>
            <div><dt className="text-slate">ID number</dt><dd className="text-ink">{maskedId}</dd></div>
            <div><dt className="text-slate">Verification</dt><dd>{customer.data ? <Badge variant={customer.data.kycStatus === "VERIFIED" ? "success" : "soft"}>{customer.data.kycStatus.toLowerCase()}</Badge> : "-"}</dd></div>
          </dl>
          {customer.data ? (
            <form className="grid gap-4 sm:grid-cols-2" onSubmit={save}>
              <Field label="Mobile number"><Input value={phoneNumber} onChange={(e) => setPhone(e.target.value)} autoComplete="tel" /></Field>
              <Field label="Address"><Input value={address} onChange={(e) => setAddress(e.target.value)} maxLength={300} autoComplete="street-address" /></Field>
              <div><Button type="submit" disabled={pending}>{pending ? "Saving…" : "Save changes"}</Button></div>
            </form>
          ) : null}
        </CardContent>
      </Card>
      <Card>
        <CardHeader><CardTitle>Security</CardTitle></CardHeader>
        <CardContent className="space-y-2 text-sm text-slate">
          <p>Your session is protected by HttpOnly cookies and expires after 15 minutes of inactivity; signing out revokes it immediately.</p>
          <p>Paying a new account always asks for your password again. Loretta Bank will never ask for your password or one-time PIN by phone, email or chat.</p>
        </CardContent>
      </Card>
      <Card>
        <CardHeader><CardTitle>AI features and privacy</CardTitle></CardHeader>
        <CardContent className="space-y-2 text-sm text-slate">
          <p>The assistant reads your accounts and transactions with exactly your own permissions. It can prepare payments, but only you can confirm them.</p>
          <p>Personal identifiers are masked before text is sent to an AI provider, and providers may not use your data to train models.</p>
        </CardContent>
      </Card>
    </div>
  );
}
