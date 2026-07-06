"use client";

import { useSearchParams } from "next/navigation";
import type { FieldStatus } from "@/lib/api/types";
import { AdminFieldList } from "@/components/admin/admin-field-list";

export function AdminFieldsPanel() {
  const searchParams = useSearchParams();
  const page = parsePage(searchParams.get("page"));
  const status = parseStatus(searchParams.get("status"));

  return <AdminFieldList page={page} status={status} />;
}

function parsePage(value: string | null) {
  const page = Number.parseInt(value ?? "1", 10);
  return Number.isFinite(page) && page > 0 ? page - 1 : 0;
}

function parseStatus(value: string | null): FieldStatus {
  return value === "APPROVED" || value === "REJECTED" ? value : "PENDING";
}
