import type { ReactNode } from "react";

interface SectionHeaderProps {
  title: string;
  description?: string;
  action?: ReactNode;
}

export function SectionHeader({ title, description, action }: SectionHeaderProps) {
  return (
    <div className="mb-6 flex flex-wrap items-start justify-between gap-3">
      <div className="space-y-1">
        <h1 className="font-display text-2xl font-semibold text-ink md:text-3xl">{title}</h1>
        {description ? <p className="text-sm text-slate">{description}</p> : null}
      </div>
      {action}
    </div>
  );
}

