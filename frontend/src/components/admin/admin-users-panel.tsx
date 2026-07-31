"use client";

import { useSearchParams } from "next/navigation";
import { AdminUserList } from "@/components/admin/admin-user-list";

export function AdminUsersPanel() {
  const searchParams = useSearchParams();
  const page = parsePage(searchParams.get("page"));
  const phoneNumber = searchParams.get("phoneNumber") ?? "";

  return <AdminUserList key={phoneNumber} page={page} phoneNumber={phoneNumber} />;
}

function parsePage(value: string | null) {
  const page = Number.parseInt(value ?? "1", 10);
  return Number.isFinite(page) && page > 0 ? page - 1 : 0;
}
