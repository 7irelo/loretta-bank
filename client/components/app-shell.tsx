"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useEffect, useMemo, useState, type ComponentType, type ReactNode } from "react";
import {
  ArrowLeftRight,
  Bell,
  FileText,
  LayoutDashboard,
  LogOut,
  Menu,
  ReceiptText,
  Settings,
  ShieldCheck,
  Sparkles,
  Wallet,
  X
} from "lucide-react";

import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { authApi } from "@/lib/api";
import { useCustomer, useUnreadCount } from "@/lib/hooks";
import { cn, getInitials } from "@/lib/utils";
import { useAuthStore } from "@/store/auth-store";
import type { UserRole } from "@/types/api";

interface NavigationItem {
  href: string;
  label: string;
  icon: ComponentType<{ className?: string }>;
  roles: UserRole[];
}

const navigation: NavigationItem[] = [
  { href: "/dashboard", label: "Home", icon: LayoutDashboard, roles: ["CUSTOMER"] },
  { href: "/accounts", label: "Accounts", icon: Wallet, roles: ["CUSTOMER"] },
  { href: "/transfer", label: "Pay & transfer", icon: ArrowLeftRight, roles: ["CUSTOMER"] },
  { href: "/transactions", label: "Transactions", icon: ReceiptText, roles: ["CUSTOMER"] },
  { href: "/statements", label: "Statements", icon: FileText, roles: ["CUSTOMER"] },
  { href: "/assistant", label: "Assistant", icon: Sparkles, roles: ["CUSTOMER"] },
  { href: "/notifications", label: "Notifications", icon: Bell, roles: ["CUSTOMER"] },
  { href: "/admin", label: "Back office", icon: ShieldCheck, roles: ["ADMIN", "SUPPORT"] },
  { href: "/settings", label: "Settings", icon: Settings, roles: ["CUSTOMER", "ADMIN", "SUPPORT"] }
];

export function AppShell({ children }: { children: ReactNode }) {
  const pathname = usePathname();
  const router = useRouter();
  const user = useAuthStore((s) => s.user);
  const hydrated = useAuthStore((s) => s.hydrated);
  const setUser = useAuthStore((s) => s.setUser);
  const [mobileOpen, setMobileOpen] = useState(false);
  const isCustomer = user?.role === "CUSTOMER";
  const customer = useCustomer();
  const unread = useUnreadCount();

  useEffect(() => {
    if (hydrated && !user) {
      router.replace("/login");
    }
  }, [hydrated, router, user]);

  useEffect(() => {
    if (isCustomer && customer.isSuccess && customer.data === null && pathname !== "/onboarding") {
      router.replace("/onboarding");
    }
  }, [customer.data, customer.isSuccess, isCustomer, pathname, router]);

  useEffect(() => setMobileOpen(false), [pathname]);

  const items = useMemo(
    () => navigation.filter((item) => user && item.roles.includes(user.role)),
    [user]
  );

  if (!hydrated || !user) {
    return (
      <div className="flex min-h-screen items-center justify-center" aria-busy="true">
        <div className="h-10 w-10 animate-spin rounded-full border-4 border-primary/20 border-t-primary" />
      </div>
    );
  }

  const signOut = async () => {
    await authApi.logout().catch(() => null);
    setUser(null);
    router.replace("/login");
  };

  const unreadCount = unread.data?.unread ?? 0;

  return (
    <div className="min-h-screen md:grid md:grid-cols-[248px_1fr]">
      <aside
        className={cn(
          "fixed inset-y-0 left-0 z-30 w-64 border-r border-line bg-white p-4 transition-transform md:static md:w-auto md:translate-x-0",
          mobileOpen ? "translate-x-0" : "-translate-x-full"
        )}
        aria-label="Main navigation"
      >
        <div className="mb-6 flex items-center justify-between">
          <Link href={isCustomer ? "/dashboard" : "/admin"} className="flex items-center gap-2">
            <span className="grid h-9 w-9 place-items-center rounded-xl bg-primary text-sm font-bold text-white">
              LB
            </span>
            <span className="font-display text-sm font-semibold text-ink">Loretta Bank</span>
          </Link>
          <button className="md:hidden" onClick={() => setMobileOpen(false)} aria-label="Close menu">
            <X className="h-5 w-5 text-slate" />
          </button>
        </div>
        <nav className="space-y-1">
          {items.map((item) => {
            const Icon = item.icon;
            const active = pathname === item.href || pathname.startsWith(`${item.href}/`);
            return (
              <Link
                key={item.href}
                href={item.href}
                aria-current={active ? "page" : undefined}
                className={cn(
                  "flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-medium transition",
                  active ? "bg-primary text-white" : "text-slate hover:bg-surface hover:text-ink"
                )}
              >
                <Icon className="h-4 w-4" />
                <span className="flex-1">{item.label}</span>
                {item.href === "/notifications" && unreadCount > 0 ? (
                  <span className="rounded-full bg-danger px-1.5 text-xs font-semibold text-white">
                    {unreadCount > 99 ? "99+" : unreadCount}
                  </span>
                ) : null}
              </Link>
            );
          })}
        </nav>
      </aside>

      {mobileOpen ? (
        <button
          onClick={() => setMobileOpen(false)}
          className="fixed inset-0 z-20 bg-ink/35 md:hidden"
          aria-label="Close navigation"
        />
      ) : null}

      <div className="flex min-h-screen min-w-0 flex-col">
        <header className="sticky top-0 z-10 flex h-16 items-center justify-between border-b border-line bg-white/90 px-4 backdrop-blur md:px-8">
          <button
            className="rounded-lg border border-line bg-white p-2 md:hidden"
            onClick={() => setMobileOpen(true)}
            aria-label="Open menu"
          >
            <Menu className="h-5 w-5 text-slate" />
          </button>
          <div className="ml-auto flex items-center gap-3">
            {user.role !== "CUSTOMER" ? <Badge variant="success">{user.role}</Badge> : null}
            <div className="hidden text-right md:block">
              <p className="text-sm font-semibold text-ink">{user.fullName}</p>
              <p className="text-xs text-slate">{user.email}</p>
            </div>
            <span className="grid h-9 w-9 place-items-center rounded-full bg-primary/10 text-sm font-semibold text-primary">
              {getInitials(user.fullName)}
            </span>
            <Button size="sm" variant="secondary" className="gap-2" onClick={signOut}>
              <LogOut className="h-4 w-4" />
              <span className="hidden sm:inline">Sign out</span>
            </Button>
          </div>
        </header>
        <main className="flex-1 p-4 md:p-8">{children}</main>
      </div>
    </div>
  );
}
