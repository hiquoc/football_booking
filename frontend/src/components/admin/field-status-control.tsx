"use client";

import { LoaderCircle } from "lucide-react";
import type { FieldStatus } from "@/lib/api/types";
import { useUpdateFieldStatus } from "@/lib/hooks/use-fields";

export function FieldStatusControl({ fieldId, status }: { fieldId: string; status: FieldStatus }) {
  const mutation = useUpdateFieldStatus(fieldId);
  return (
    <div className="flex flex-wrap items-center gap-2">
      <select
        value={mutation.data?.status ?? status}
        disabled={mutation.isPending}
        onChange={(event) => mutation.mutate(event.target.value as FieldStatus)}
        className="rounded-full border border-slate-200 bg-white px-4 py-2 text-sm font-black text-slate-700 outline-none focus:border-sky-500"
        aria-label="Đổi trạng thái sân"
      >
        <option value="PENDING">Chờ xác nhận</option>
        <option value="APPROVED">Phê duyệt</option>
        <option value="REJECTED">Từ chối</option>
      </select>
      {mutation.isPending ? <LoaderCircle className="size-4 animate-spin text-sky-600" /> : null}
      {mutation.error ? <span className="text-xs font-semibold text-rose-600">{mutation.error.message}</span> : null}
    </div>
  );
}
