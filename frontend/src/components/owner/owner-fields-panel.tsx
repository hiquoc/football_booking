"use client";

import { useSearchParams } from "next/navigation";
import { OwnerFieldList } from "@/components/owner/owner-field-list";

export function OwnerFieldsPanel({ role }: { role: "OWNER" | "EMPLOYEE" }) {
  const searchParams = useSearchParams();
  const page = parsePage(searchParams.get("page"));

  return <OwnerFieldList page={page} role={role} />;
}

function parsePage(value: string | null) {
  const page = Number.parseInt(value ?? "1", 10);
  return Number.isFinite(page) && page > 0 ? page - 1 : 0;
}
