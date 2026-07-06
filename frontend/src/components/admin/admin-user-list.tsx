"use client";

import { Mail, Phone, UserRound } from "lucide-react";
import type { User } from "@/lib/api/types";
import { useUpdateUserRole, useUsers } from "@/lib/hooks/use-users";
import { DataEmpty, DataError, ListSkeleton } from "@/components/ui/data-state";
import { AdminPagination } from "./admin-pagination";

const roleLabels = {
  CLIENT: "Khách hàng",
  OWNER: "Chủ sân",
  ADMIN: "Quản trị viên",
};

export function AdminUserList({ page }: { page: number }) {
  const query = useUsers(page);
  const roleMutation = useUpdateUserRole(page);

  if (query.isPending) return <div className="mt-8"><ListSkeleton /></div>;
  if (query.isError)
    return <div className="mt-8"><DataError title="Không thể tải danh sách người dùng" /></div>;
  if (!query.data.content.length)
    return (
      <div className="mt-8">
        <DataEmpty
          title="Chưa có người dùng"
          description="Danh sách tài khoản hiện đang trống."
        />
      </div>
    );

  return (
    <section className="mt-8">
      <p className="mb-4 text-sm font-semibold text-slate-500">
        Tổng cộng {query.data.totalElements} tài khoản
      </p>
      {roleMutation.error ? (
        <p role="alert" className="mb-4 rounded-xl bg-rose-50 p-3 text-sm font-semibold text-rose-700">
          Không thể cập nhật vai trò. Dữ liệu trước đó đã được khôi phục.
        </p>
      ) : null}
      <div className="space-y-4">
        {query.data.content.map((user) => (
          <article
            key={user.id}
            className="grid gap-4 rounded-[1.5rem] border border-slate-200 bg-white p-5 shadow-sm sm:grid-cols-[minmax(0,1fr)_auto] sm:items-center"
          >
            <div className="flex min-w-0 gap-4">
              <span className="grid size-11 shrink-0 place-items-center rounded-full bg-sky-100 text-sky-700">
                <UserRound className="size-5" />
              </span>
              <div className="min-w-0">
                <h2 className="truncate font-black text-slate-950">
                  {user.fullName || "Chưa cập nhật tên"}
                </h2>
                <div className="mt-2 flex flex-wrap gap-x-5 gap-y-1 text-sm text-slate-500">
                  <span className="inline-flex items-center gap-1.5">
                    <Phone className="size-3.5" /> {user.phoneNumber}
                  </span>
                  {user.email ? (
                    <span className="inline-flex items-center gap-1.5">
                      <Mail className="size-3.5" /> {user.email}
                    </span>
                  ) : null}
                </div>
              </div>
            </div>
            <div className="flex gap-2 sm:flex-col sm:items-end">
              <select
                aria-label={`Vai trò của ${user.fullName || user.phoneNumber}`}
                className="rounded-full border border-slate-200 bg-slate-50 px-3 py-1 text-xs font-black text-slate-700"
                value={user.userType}
                disabled={roleMutation.isPending}
                onChange={(event) =>
                  roleMutation.mutate({
                    id: user.id,
                    userType: event.target.value as User["userType"],
                  })
                }
              >
                {Object.entries(roleLabels).map(([value, label]) => (
                  <option key={value} value={value}>{label}</option>
                ))}
              </select>
              <span className="text-xs font-semibold text-slate-400">
                {user.status}
              </span>
            </div>
          </article>
        ))}
      </div>
      <AdminPagination
        currentPage={query.data.page}
        totalPages={query.data.totalPages}
        pathname="/admin/users"
      />
    </section>
  );
}
