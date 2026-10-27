"use client";

import { useQuery, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";

import { EmptyState } from "@/components/form";
import { SectionHeader } from "@/components/section-header";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { notificationsApi } from "@/lib/api";
import { keys } from "@/lib/hooks";
import { cn, formatDateTime } from "@/lib/utils";

export default function NotificationsPage() {
  const queryClient = useQueryClient();
  const [page, setPage] = useState(0);
  const notifications = useQuery({ queryKey: ["notifications", page], queryFn: () => notificationsApi.list(page) });

  const refresh = async () => {
    await queryClient.invalidateQueries({ queryKey: ["notifications"] });
    await queryClient.invalidateQueries({ queryKey: keys.unread });
  };

  return (
    <div className="space-y-6">
      <SectionHeader
        title="Notifications"
        description="Payments, deposits and account updates."
        action={<Button variant="secondary" onClick={async () => { await notificationsApi.markAllRead(); await refresh(); }}>Mark all as read</Button>}
      />
      <Card>
        <CardContent>
          {notifications.isLoading ? <Skeleton className="h-40 w-full" /> : (notifications.data?.content ?? []).length === 0 ? (
            <EmptyState title="You're all caught up" />
          ) : (
            <ul className="divide-y divide-line">
              {notifications.data?.content.map((n) => (
                <li key={n.id}>
                  <button
                    className={cn("flex w-full items-start gap-3 py-3 text-left", !n.readAt && "font-medium")}
                    onClick={async () => { if (!n.readAt) { await notificationsApi.markRead(n.id); await refresh(); } }}
                  >
                    <span className={cn("mt-2 h-2 w-2 shrink-0 rounded-full", n.readAt ? "bg-transparent" : "bg-primary")} aria-hidden />
                    <span className="min-w-0 flex-1">
                      <span className="block text-ink">{n.subject}</span>
                      <span className="block text-sm font-normal text-slate">{n.body}</span>
                    </span>
                    <span className="shrink-0 text-xs font-normal text-muted">{formatDateTime(n.createdAt)}</span>
                  </button>
                </li>
              ))}
            </ul>
          )}
          <div className="mt-4 flex justify-end gap-2">
            <Button size="sm" variant="secondary" disabled={page === 0} onClick={() => setPage((p) => p - 1)}>Newer</Button>
            <Button size="sm" variant="secondary" disabled={notifications.data?.last ?? true} onClick={() => setPage((p) => p + 1)}>Older</Button>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
