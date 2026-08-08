"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { AlertTriangle, Eye, Mail, Phone, Search, ShieldBan, ShieldCheck, UserRound } from "lucide-react";
import type { CommunityViolation, FieldViolation, ModerationAuditLog, User } from "@/lib/api/types";
import {
  useCurrentUser,
  useUpdateUserRole,
  useUpdateUserStatus,
  useUserViolations,
  useUsers,
} from "@/lib/hooks/use-users";
import { DataEmpty, DataError, ListSkeleton } from "@/components/ui/data-state";
import { AdminPagination } from "./admin-pagination";

const roleLabels: Record<User["userType"], string> = {
  CLIENT: "Khách hàng",
  OWNER: "Chủ sân",
  EMPLOYEE: "Nhân viên",
  ADMIN: "Quản trị viên",
};

const assignableRoleLabels = Object.entries(roleLabels).filter(([value]) => value !== "ADMIN");

export function AdminUserList({
  page,
  phoneNumber,
}: {
  page: number;
  phoneNumber: string;
}) {
  const router = useRouter();
  const [phoneInput, setPhoneInput] = useState(phoneNumber);
  const [expandedUserId, setExpandedUserId] = useState<string | null>(null);
  const query = useUsers(page, 10, phoneNumber);
  const currentUserQuery = useCurrentUser();
  const roleMutation = useUpdateUserRole();
  const statusMutation = useUpdateUserStatus();

  useEffect(() => {
    const timeout = window.setTimeout(() => {
      const trimmedPhone = phoneInput.trim();
      if (trimmedPhone === phoneNumber) return;
      const params = new URLSearchParams();
      if (trimmedPhone) params.set("phoneNumber", trimmedPhone);
      router.replace(`/admin/users${params.size ? `?${params}` : ""}`);
    }, 500);

    return () => window.clearTimeout(timeout);
  }, [phoneInput, phoneNumber, router]);

  const currentUserId = currentUserQuery.data?.id;

  if (query.isPending) {
    return (
      <section className="mt-8">
        <UserSearch value={phoneInput} onChange={setPhoneInput} />
        <div className="mt-8"><ListSkeleton /></div>
      </section>
    );
  }

  if (query.isError) {
    return (
      <section className="mt-8">
        <UserSearch value={phoneInput} onChange={setPhoneInput} />
        <div className="mt-8"><DataError title="Không thể tải danh sách người dùng" /></div>
      </section>
    );
  }

  if (!query.data.content.length) {
    return (
      <section className="mt-8">
        <UserSearch value={phoneInput} onChange={setPhoneInput} />
        <DataEmpty
          title="Chưa có người dùng"
          description="Không có tài khoản nào khớp bộ lọc hiện tại."
        />
      </section>
    );
  }

  return (
    <section className="mt-8">
      <UserSearch value={phoneInput} onChange={setPhoneInput} />
      <p className="mb-4 text-sm font-semibold text-slate-500">
        Tổng cộng {query.data.totalElements} tài khoản
      </p>
      {roleMutation.error ? (
        <p role="alert" className="mb-4 rounded-lg bg-rose-50 p-3 text-sm font-semibold text-rose-700">
          Không thể cập nhật vai trò. Dữ liệu trước đó đã được khôi phục.
        </p>
      ) : null}
      {statusMutation.error ? (
        <p role="alert" className="mb-4 rounded-lg bg-rose-50 p-3 text-sm font-semibold text-rose-700">
          Không thể cập nhật trạng thái cấm. Dữ liệu trước đó đã được khôi phục.
        </p>
      ) : null}
      <div className="space-y-4">
        {query.data.content.map((user) => {
          const isCurrentUser = user.id === currentUserId;
          const banned = user.status === "PLATFORM_BANNED" || Boolean(user.isPermanentBan);
          const roleOptions = user.userType === "ADMIN" ? Object.entries(roleLabels) : assignableRoleLabels;

          return (
            <article key={user.id} className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
              <div className="grid gap-4 sm:grid-cols-[minmax(0,1fr)_auto] sm:items-center">
                <div className="flex min-w-0 gap-4">
                  <span className="grid size-11 shrink-0 place-items-center rounded-full bg-green-50 text-green-700">
                    <UserRound className="size-5" />
                  </span>
                  <div className="min-w-0">
                    <div className="flex flex-wrap items-center gap-2">
                      <Link href={`/users/${user.id}/profile`} className="truncate font-black text-slate-950 hover:text-green-700">
                        {user.fullName || "Chưa cập nhật tên"}
                      </Link>
                      {isCurrentUser ? (
                        <span className="rounded-full bg-slate-100 px-2 py-0.5 text-xs font-black text-slate-600">
                          Bạn
                        </span>
                      ) : null}
                      {banned ? (
                        <span className="rounded-full bg-rose-100 px-2 py-0.5 text-xs font-black text-rose-700">
                          Bị cấm
                        </span>
                      ) : null}
                    </div>
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
                <div className="flex flex-wrap gap-2 sm:justify-end">
                  <select
                    aria-label={`Vai trò của ${user.fullName || user.phoneNumber}`}
                    className="h-10 rounded-xl border border-slate-200 bg-slate-50 px-3 text-xs font-black text-slate-700 outline-none focus:border-green-500 focus:ring-4 focus:ring-green-100 disabled:cursor-not-allowed disabled:opacity-60"
                    value={user.userType}
                    disabled={isCurrentUser || roleMutation.isPending}
                    title={isCurrentUser ? "Không thể đổi vai trò của chính mình" : undefined}
                    onChange={(event) =>
                      roleMutation.mutate({
                        id: user.id,
                        userType: event.target.value as User["userType"],
                      })
                    }
                  >
                    {roleOptions.map(([value, label]) => (
                      <option key={value} value={value}>{label}</option>
                    ))}
                  </select>
                  <button
                    type="button"
                    className={`inline-flex h-10 items-center gap-2 rounded-xl px-4 text-sm font-black text-white disabled:cursor-not-allowed disabled:opacity-60 ${banned ? "bg-green-600 hover:bg-green-700" : "bg-rose-600 hover:bg-rose-700"}`}
                    disabled={isCurrentUser || statusMutation.isPending}
                    title={isCurrentUser ? "Không thể cấm hoặc bỏ cấm chính mình" : undefined}
                    onClick={() =>
                      statusMutation.mutate({
                        id: user.id,
                        status: banned ? "ACTIVE" : "PLATFORM_BANNED",
                      })
                    }
                  >
                    {banned ? <ShieldCheck className="size-4" /> : <ShieldBan className="size-4" />}
                    {banned ? "Bỏ cấm" : "Cấm"}
                  </button>
                  <button
                    type="button"
                    className="inline-flex h-10 items-center gap-2 rounded-xl border border-slate-200 bg-white px-4 text-sm font-black text-slate-700 hover:border-green-300 hover:text-green-700"
                    onClick={() => setExpandedUserId(expandedUserId === user.id ? null : user.id)}
                  >
                    <Eye className="size-4" />
                    Vi phạm
                  </button>
                  {/* <span className="inline-flex h-10 items-center rounded-full bg-slate-100 px-3 text-xs font-semibold text-slate-500">
                    {user.status}
                  </span> */}
                </div>
              </div>
              {expandedUserId === user.id ? <ViolationPanel userId={user.id} /> : null}
            </article>
          );
        })}
      </div>
      <AdminPagination
        currentPage={query.data.page}
        totalPages={query.data.totalPages}
        pathname="/admin/users"
        params={phoneNumber ? { phoneNumber } : {}}
      />
    </section>
  );
}

function UserSearch({
  value,
  onChange,
}: {
  value: string;
  onChange: (value: string) => void;
}) {
  return (
    <label className="mb-6 block max-w-md">
      <span className="mb-2 block text-sm font-black text-slate-700">Lọc theo số điện thoại</span>
      <span className="flex h-11 items-center gap-2 rounded-xl border border-slate-200 bg-white px-4 shadow-sm focus-within:border-green-500 focus-within:ring-4 focus-within:ring-green-100">
        <Search className="size-4 text-slate-400" />
        <input
          value={value}
          onChange={(event) => onChange(event.target.value)}
          placeholder="Nhập số điện thoại"
          className="min-w-0 flex-1 bg-transparent text-sm font-semibold text-slate-900 outline-none placeholder:text-slate-400"
        />
      </span>
    </label>
  );
}

function ViolationPanel({ userId }: { userId: string }) {
  const query = useUserViolations(userId);

  if (query.isPending) {
    return (
      <div className="mt-4 rounded-lg bg-slate-50 p-4 text-sm font-semibold text-slate-500">
        Đang tải trạng thái vi phạm...
      </div>
    );
  }

  if (query.isError) {
    return (
      <div className="mt-4 rounded-lg bg-rose-50 p-4 text-sm font-semibold text-rose-700">
        Không thể tải thông tin vi phạm.
      </div>
    );
  }

  const communityViolations = query.data.community.content;
  const fieldViolations = query.data.field.content;
  const auditLogs = query.data.audit.content;
  const activeCommunityCount = communityViolations.filter((violation) =>
    violation.status === "ACTIVE" || violation.status === "PERMANENT"
  ).length;
  const activeFieldCount = fieldViolations.filter((violation) =>
    violation.violationCount > 0 || violation.banned
  ).length;
  const activeCount = activeCommunityCount + activeFieldCount;
  const totalViolations = query.data.community.totalElements + query.data.field.totalElements;

  return (
    <div className="mt-4 rounded-lg bg-slate-50 p-4">
      <div className="flex flex-wrap items-center gap-3 text-sm">
        <span className="inline-flex items-center gap-2 font-black text-slate-800">
          <AlertTriangle className="size-4 text-amber-600" />
          Trạng thái: {activeCount > 0 ? `${activeCount} đang hiệu lực` : "Không có vi phạm đang hiệu lực"}
        </span>
        <span className="text-slate-500">Tổng cộng {totalViolations} bản ghi vi phạm</span>
      </div>
      {communityViolations.length > 0 || fieldViolations.length > 0 ? (
        <ViolationDetails communityViolations={communityViolations} fieldViolations={fieldViolations} auditLogs={auditLogs} />
      ) : (
        <p className="mt-3 text-sm text-slate-500">Chưa có vi phạm nào.</p>
      )}
    </div>
  );
}

function ViolationDetails({
  communityViolations,
  fieldViolations,
  auditLogs,
}: {
  communityViolations: CommunityViolation[];
  fieldViolations: FieldViolation[];
  auditLogs: ModerationAuditLog[];
}) {
  return (
    <div className="mt-3 space-y-4 border-t border-slate-200 pt-3 text-sm text-slate-600">
      {fieldViolations.length > 0 ? (
        <section>
          <h3 className="text-xs font-black uppercase text-slate-500">Vắng mặt đặt sân</h3>
          <div className="mt-2 space-y-2 text-xs font-semibold text-slate-500">
            {fieldViolations.map((violation) => (
              <div key={violation.id} className="flex flex-wrap gap-3">
                <span>
                  Sân:{" "}
                  <Link
                    href={`/fields/${encodeURIComponent(violation.fieldId)}`}
                    className="font-black text-green-700 underline-offset-2 hover:underline"
                  >
                    {violation.fieldId}
                  </Link>
                </span>
                <span>Số lần: {violation.violationCount}</span>
                <span>Trạng thái: {violation.banned ? "Bị cấm đặt sân" : "Cảnh báo"}</span>
                {violation.lastViolationDate ? <span>Lần cuối: {formatDateTime(violation.lastViolationDate)}</span> : null}
                {violation.banDate ? <span>Ngày cấm: {formatDateTime(violation.banDate)}</span> : null}
              </div>
            ))}
          </div>
        </section>
      ) : null}
      {communityViolations.length > 0 ? (
        <section>
          <h3 className="text-xs font-black uppercase text-slate-500">Bài đăng cộng đồng</h3>
          <div className="mt-2 space-y-2 text-xs font-semibold text-slate-500">
            {communityViolations.map((violation) => (
              <div key={violation.id} className="flex flex-wrap gap-3">
                <span>Lý do: {violation.reason}</span>
                <span>Hành động: {violation.action}</span>
                <span>Trạng thái: {violation.status}</span>
                <span>Ngày tạo: {formatDateTime(violation.createdAt)}</span>
                {violation.expireAt ? <span>Hết hạn: {formatDateTime(violation.expireAt)}</span> : null}
              </div>
            ))}
          </div>
        </section>
      ) : null}
      {auditLogs.length > 0 ? (
        <section>
          <h3 className="text-xs font-black uppercase text-slate-500">Nhật ký kiểm duyệt</h3>
          <div className="mt-2 space-y-2 text-xs font-semibold text-slate-500">
            {auditLogs.map((log) => (
              <div key={log.id} className="flex flex-wrap gap-3">
                <span>Hành động: {log.action}</span>
                {log.details ? <span>Chi tiết: {log.details}</span> : null}
                <span>Ngày tạo: {formatDateTime(log.createdAt)}</span>
              </div>
            ))}
          </div>
        </section>
      ) : null}
    </div>
  );
}

function formatDateTime(value: string) {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return new Intl.DateTimeFormat("vi-VN", {
    dateStyle: "short",
    timeStyle: "short",
  }).format(date);
}
