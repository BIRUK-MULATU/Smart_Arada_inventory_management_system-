import type { HTMLAttributes } from "react";

export function Card({ className = "", ...props }: HTMLAttributes<HTMLDivElement>) {
  return (
    <div
      className={`rounded-lg border border-ink-200 bg-white p-4 shadow-sm transition-shadow duration-150
        hover:shadow-md ${className}`}
      {...props}
    />
  );
}
