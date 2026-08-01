"use client";

import Link from "next/link";
import { ArrowLeft } from "lucide-react";

export function BackLink({
  href,
  children,
  className = "",
}: {
  href: string;
  children: React.ReactNode;
  className?: string;
}) {
  return (
    <Link
      href={href}
      className={`inline-flex items-center gap-2 text-sm font-bold text-slate-500 transition hover:text-green-600 ${className}`.trim()}
    >
      <ArrowLeft className="size-4" />
      {children}
    </Link>
  );
}
