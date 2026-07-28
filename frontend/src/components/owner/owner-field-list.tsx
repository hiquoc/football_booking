"use client";

import Link from "next/link";
import { Eye, MapPin, Pencil, Settings2, Users } from "lucide-react";
import type { FieldStatus } from "@/lib/api/types";
import { formatFieldAddress } from "@/lib/field-format";
import { useManagedFields } from "@/lib/hooks/use-owner-fields";
import { DataEmpty, DataError, ListSkeleton } from "@/components/ui/data-state";
import { AdminPagination } from "@/components/admin/admin-pagination";

const statusLabels: Record<FieldStatus, string> = {
  PENDING: "Chờ xác nhận",
  APPROVED: "Đã xác nhận",
  REJECTED: "Đã từ chối",
};

const statusStyles: Record<FieldStatus, string> = {
  PENDING: "bg-amber-100 text-amber-700",
  APPROVED: "bg-sky-100 text-sky-700",
  REJECTED: "bg-rose-100 text-rose-700",
};

export function OwnerFieldList({ page, role }: { page: number; role: "OWNER" | "EMPLOYEE" }) {
  const query = useManagedFields(role, page);

  if (query.isPending) return <ListSkeleton />;
  if (query.isError) return <DataError title="Không thể tải danh sách sân" />;
  if (!query.data.content.length)
    return (
      <DataEmpty
        title="Bạn chưa có sân nào"
        description="Tạo sân đầu tiên để bắt đầu thiết lập sân con, giá và lịch hoạt động."
      />
    );

  return (
    <section>
      <p className="mb-4 text-sm font-semibold text-slate-500">
        Tổng cộng {query.data.totalElements} sân
      </p>
      <div className="space-y-4">
        {query.data.content.map((field) => (
          <article
            key={field.id}
            className="rounded-[1.5rem] border border-slate-200 bg-white p-5 shadow-sm transition hover:border-sky-200 hover:shadow-md"
          >
            <div className="flex flex-col justify-between gap-5 sm:flex-row sm:items-start">
              <div className="min-w-0">
                <div className="flex flex-wrap items-center gap-2">
                  <span
                    className={`inline-flex rounded-full px-3 py-1 text-xs font-black ${statusStyles[field.status]}`}
                  >
                    {statusLabels[field.status]}
                  </span>
                  {!field.active ? (
                    <span className="inline-flex rounded-full bg-slate-100 px-3 py-1 text-xs font-black text-slate-600">
                      Tạm ngưng
                    </span>
                  ) : null}
                </div>
                <h2 className="mt-3 truncate text-xl font-black text-slate-950">
                  {field.name}
                </h2>
                <p className="mt-2 flex items-start gap-2 text-sm text-slate-500">
                  <MapPin className="mt-0.5 size-4 shrink-0 text-sky-600" />
                  {formatFieldAddress(field)}
                </p>
                <Link
                  href={`/owner/fields/${field.id}/employees`}
                  className="inline-flex items-center gap-2 rounded-full border border-slate-200 bg-white px-4 py-2.5 text-sm font-black text-slate-700 transition hover:border-sky-400 hover:text-sky-700"
                >
                  <Users className="size-4" /> NhÃ¢n viÃªn
                </Link>
              </div>
              <div className="flex shrink-0 flex-wrap gap-2">
                <Link
                  href={`/fields/${field.id}`}
                  className="inline-flex items-center gap-2 rounded-full border border-slate-200 bg-white px-4 py-2.5 text-sm font-black text-slate-700 transition hover:border-sky-400 hover:text-sky-700"
                >
                  <Eye className="size-4" /> Xem chi tiết
                </Link>
                <Link
                  href={`/owner/fields/${field.id}/edit`}
                  className="inline-flex items-center gap-2 rounded-full border border-slate-200 bg-white px-4 py-2.5 text-sm font-black text-slate-700 transition hover:border-sky-400 hover:text-sky-700"
                >
                  <Pencil className="size-4" /> Chỉnh sửa
                </Link>
                <Link
                  href={`/owner/fields/${field.id}/sub-fields`}
                  className="inline-flex items-center gap-2 rounded-full bg-slate-950 px-4 py-2.5 text-sm font-black text-white transition hover:bg-sky-600"
                >
                  <Settings2 className="size-4" /> Quản lý
                </Link>
              </div>
            </div>
          </article>
        ))}
      </div>
      <AdminPagination
        currentPage={query.data.page}
        totalPages={query.data.totalPages}
        pathname="/owner/fields"
      />
    </section>
  );
}
