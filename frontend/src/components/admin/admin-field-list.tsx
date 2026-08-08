"use client";

import Link from "next/link";
import { MapPin } from "lucide-react";
import type { FieldStatus } from "@/lib/api/types";
import { formatFieldAddress } from "@/lib/field-format";
import { useFields } from "@/lib/hooks/use-fields";
import { DataEmpty, DataError, ListSkeleton } from "@/components/ui/data-state";
import { AdminPagination } from "./admin-pagination";
import { FieldStatusControl } from "./field-status-control";

const filters: Array<{ status: FieldStatus; label: string }> = [
  { status: "PENDING", label: "Chờ xác nhận" },
  { status: "APPROVED", label: "Đã xác nhận" },
  { status: "REJECTED", label: "Đã từ chối" },
];

const statusStyles: Record<FieldStatus, string> = {
  PENDING: "bg-amber-100 text-amber-700",
  APPROVED: "bg-green-100 text-green-700",
  REJECTED: "bg-rose-100 text-rose-700",
};

export function AdminFieldList({
  page,
  status,
}: {
  page: number;
  status: FieldStatus;
}) {
  const query = useFields(page, 10, status);

  return (
    <section className="mt-8">
      <div className="mb-6 flex flex-wrap gap-2">
        {filters.map((filter) => (
          <Link
            key={filter.status}
            href={`/admin/fields?status=${filter.status}`}
            className={`rounded-xl px-4 py-2 text-sm font-black transition shadow-none ${
              status === filter.status
                ? "bg-green-600 text-white"
                : "border border-slate-200 bg-white text-slate-600 hover:border-green-300 hover:text-green-700"
            }`}
          >
            {filter.label}
          </Link>
        ))}
      </div>

      {query.isPending ? (
        <ListSkeleton />
      ) : query.isError ? (
        <DataError title="Không thể tải danh sách sân" />
      ) : !query.data.content.length ? (
        <DataEmpty
          title="Không có sân phù hợp"
          description="Chưa có sân nào thuộc trạng thái đã chọn."
        />
      ) : (
        <>
          <p className="mb-4 text-sm font-semibold text-slate-500">
            Tổng cộng {query.data.totalElements} sân
          </p>
          <div className="space-y-4">
            {query.data.content.map((field) => (
              <article
                key={field.id}
                className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm"
              >
                <div className="flex flex-col justify-between gap-4 sm:flex-row sm:items-start">
                  <div>
                    <span
                      className={`inline-flex rounded-lg px-3 py-1 text-xs font-black ${statusStyles[field.status]}`}
                    >
                      {filters.find((item) => item.status === field.status)?.label}
                    </span>
                    <h2 className="mt-3 text-xl font-black text-slate-950">
                      {field.name}
                    </h2>
                    <p className="mt-2 flex items-start gap-2 text-sm text-slate-500">
                      <MapPin className="mt-0.5 size-4 shrink-0 text-green-600" />
                      {formatFieldAddress(field)}
                    </p>
                  </div>
                  <div className="flex shrink-0 flex-wrap items-center gap-2">
                    <Link
                      href={`/fields/${field.id}`}
                      className="inline-flex items-center justify-center rounded-xl bg-green-600 px-4 py-2 text-sm font-black text-white hover:bg-green-700"
                    >
                      Xem chi tiết
                    </Link>

                    <FieldStatusControl fieldId={field.id} status={field.status} />
                  </div>
                </div>
              </article>
            ))}
          </div>
          <AdminPagination
            currentPage={query.data.page}
            totalPages={query.data.totalPages}
            pathname="/admin/fields"
            params={{ status }}
          />
        </>
      )}
    </section>
  );
}
