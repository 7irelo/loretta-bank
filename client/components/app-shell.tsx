"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useEffect, useMemo, useState, type ComponentType, type ReactNode } from "react";
import {
  ArrowLeftRight,
  LayoutDashboard,
  LogOut,
  Menu,
  ReceiptText,
  ShieldCheck,
  Wallet,
  X
} from "lucide-react";

import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { isRoleAllowed } from "@/lib/auth";
import { cn, getInitials } from "@/lib/utils";
import { useAuthStore } from "@/store/auth-store";
import type { UserRole } from "@/types/auth";

interface AppShellProps {
  children: ReactNode;
}

interface NavigationItem {
  href: string;
  label: string;
  icon: ComponentType<{ className?: string }>;
  roles: UserRole[];
}

const navigationItems: NavigationItem[] = [
  {
    href: "/dashboard",
    label: "Dashboard",
    icon: LayoutDashboard,
    roles: ["ADMIN", "CUSTOMER", "SUPPORT"]
  },
  {
    href: "/accounts",
    label: "Accounts",
    icon: Wallet,
    roles: ["ADMIN", "CUSTOMER", "SUPPORT"]
  },
  {
    href: "/transfer",
    label: "Transfers",
    icon: ArrowLeftRight,
    roles: ["ADMIN", "CUSTOMER", "SUPPORT"]
  },
  {
    href: "/transactions",
    label: "Transactions",
    icon: ReceiptText,
    roles: ["ADMIN", "CUSTOMER", "SUPPORT"]
  },
  {
    href: "/admin",
    label: "Admin",
    icon: ShieldCheck,
    roles: ["ADMIN", "SUPPORT"]
  }
];

export function AppShell({ children }: AppShellProps) {
  const pathname = usePathname();
  const router = useRouter();
  const { token, user, hydrated, clearSession } = useAuthStore((state) => ({
    token: state.token,
    user: state.user,
    hydrated: state.hydrated,
    clearSession: state.clearSession
  }));
  const [isMobileOpen, setIsMobileOpen] = useState(false);

  useEffect(() => {
    if (hydrated && !token) {
      router.replace("/login");
    }
  }, [hydrated, router, token]);

  useEffect(() => {
    setIsMobileOpen(false);
  }, [pathname]);

  const allowedItems = useMemo(() => {
    return navigationItems.filter((item) => isRoleAllowed(user?.role, item.roles));
  }, [user?.role]);

  if (!hydrated) {
    return (
      <div className="flex min-h-screen items-center justify-center">
        <div className="h-12 w-12 animate-spin rounded-full border-4 border-primary/20 border-t-primary" />
      </div>
    );
  }

  if (!token || !user) {
    return null;
  }

  const logout = () => {
    clearSession();
    router.replace("/login");
  };

  return (
    <div className="min-h-screen md:grid md:grid-cols-[260px_1fr]">
      <aside
        className={cn(
          "fixed inset-y-0 left-0 z-30 w-64 border-r border-line bg-white/95 p-4 backdrop-blur transition-transform md:static md:translate-x-0",
          isMobileOpen ? "translate-x-0" : "-translate-x-full"
        )}
      >
        <div className="mb-6 flex items-center justify-between md:justify-start">
          <div className="flex items-center gap-2">
            <div className="grid h-9 w-9 place-items-center rounded-xl bg-primary text-sm font-bold text-white">
              LB
            </div>
            <div>
              <p className="font-display text-sm font-semibold text-ink">Loretta Bank</p>
              <p className="text-xs text-slate">Digital Banking Portal</p>
            </div>
          </div>
          <button className="md:hidden" onClick={() => setIsMobileOpen(false)} aria-label="Close menu">
            <X className="h-5 w-5 text-slate" />
          </button>
        </div>

        <nav className="space-y-1">
          {allowedItems.map((item) => {
            const Icon = item.icon;
            const active = pathname === item.href || pathname.startsWith(`${item.href}/`);

            return (
              <Link
                key={item.href}
                href={item.href}
                className={cn(
                  "flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-medium transition",
                  active
                    ? "bg-primary text-white"
                    : "text-slate hover:bg-surface hover:text-ink"
                )}
              >
                <Icon className="h-4 w-4" />
                {item.label}
              </Link>
            );
          })}
        </nav>

        <div className="mt-8 rounded-2xl border border-line bg-surface p-3">
          <p className="text-xs uppercase tracking-wide text-muted">Secure Session</p>
          <p className="mt-1 text-sm text-slate">Tokens are rotated by the gateway and auto-cleared on unauthorized access.</p>
        </div>
      </aside>

      {isMobileOpen ? (
        <button
          onClick={() => setIsMobileOpen(false)}
          className="fixed inset-0 z-20 bg-ink/35 md:hidden"
          aria-label="Close navigation overlay"
        />
      ) : null}

      <div className="relative z-10 flex min-h-screen flex-col">
        <header className="sticky top-0 z-10 flex h-16 items-center justify-between border-b border-line bg-white/90 px-4 backdrop-blur md:px-8">
          <div className="flex items-center gap-2">
            <button
              className="rounded-lg border border-line bg-white p-2 md:hidden"
              onClick={() => setIsMobileOpen(true)}
              aria-label="Open menu"
            >
              <Menu className="h-5 w-5 text-slate" />
            </button>
            <span className="hidden text-sm text-slate md:inline-flex">Core Banking Workspace</span>
          </div>

          <div className="flex items-center gap-3">
            <Badge variant={user.role === "ADMIN" ? "success" : "soft"}>{user.role}</Badge>
            <div className="hidden text-right md:block">
              <p className="text-sm font-semibold text-ink">{user.fullName}</p>
              <p className="text-xs text-slate">{user.email}</p>
            </div>
            <div className="grid h-9 w-9 place-items-center rounded-full bg-primary/10 text-sm font-semibold text-primary">
              {getInitials(user.fullName)}
            </div>
            <Button size="sm" variant="secondary" className="gap-2" onClick={logout}>
              <LogOut className="h-4 w-4" />
              Sign out
            </Button>
          </div>
        </header>

        <main className="flex-1 p-4 md:p-8">{children}</main>
      </div>
    </div>
  );
}





