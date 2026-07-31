"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { AlertTriangle, Eye, Mail, Phone, Search, ShieldBan, ShieldCheck, UserRound } from "lucide-react";
import type { CommunityViolation, FieldViolation, User } from "@/lib/api/types";
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
  CLIENT: "Khach hang",
  OWNER: "Chu san",
  EMPLOYEE: "Nhan vien",
  ADMIN: "Quan tri vien",
};

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
  const roleMutation = useUpdateUserRole(page, 10, phoneNumber);
  const statusMutation = useUpdateUserStatus(page, 10, phoneNumber);

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
        <div className="mt-8"><DataError title="Khong the tai danh sach nguoi dung" /></div>
      </section>
    );
  }

  if (!query.data.content.length) {
    return (
      <section className="mt-8">
        <UserSearch value={phoneInput} onChange={setPhoneInput} />
        <DataEmpty
          title="Chua co nguoi dung"
          description="Khong co tai khoan nao khop bo loc hien tai."
        />
      </section>
    );
  }

  return (
    <section className="mt-8">
      <UserSearch value={phoneInput} onChange={setPhoneInput} />
      <p className="mb-4 text-sm font-semibold text-slate-500">
        Tong cong {query.data.totalElements} tai khoan
      </p>
      {roleMutation.error ? (
        <p role="alert" className="mb-4 rounded-lg bg-rose-50 p-3 text-sm font-semibold text-rose-700">
          Khong the cap nhat vai tro. Du lieu truoc do da duoc khoi phuc.
        </p>
      ) : null}
      {statusMutation.error ? (
        <p role="alert" className="mb-4 rounded-lg bg-rose-50 p-3 text-sm font-semibold text-rose-700">
          Khong the cap nhat trang thai cam. Du lieu truoc do da duoc khoi phuc.
        </p>
      ) : null}
      <div className="space-y-4">
        {query.data.content.map((user) => {
          const isCurrentUser = user.id === currentUserId;
          const banned = user.status === "PLATFORM_BANNED" || Boolean(user.isPermanentBan);

          return (
            <article
              key={user.id}
              className="rounded-lg border border-slate-200 bg-white p-5 shadow-sm"
            >
              <div className="grid gap-4 sm:grid-cols-[minmax(0,1fr)_auto] sm:items-center">
                <div className="flex min-w-0 gap-4">
                  <span className="grid size-11 shrink-0 place-items-center rounded-full bg-sky-100 text-sky-700">
                    <UserRound className="size-5" />
                  </span>
                  <div className="min-w-0">
                    <div className="flex flex-wrap items-center gap-2">
                      <h2 className="truncate font-black text-slate-950">
                        {user.fullName || "Chua cap nhat ten"}
                      </h2>
                      {isCurrentUser ? (
                        <span className="rounded-full bg-slate-100 px-2 py-0.5 text-xs font-black text-slate-600">
                          Ban
                        </span>
                      ) : null}
                      {banned ? (
                        <span className="rounded-full bg-rose-100 px-2 py-0.5 text-xs font-black text-rose-700">
                          Bi cam
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
                    aria-label={`Vai tro cua ${user.fullName || user.phoneNumber}`}
                    className="h-10 rounded-full border border-slate-200 bg-slate-50 px-3 text-xs font-black text-slate-700 disabled:cursor-not-allowed disabled:opacity-60"
                    value={user.userType}
                    disabled={isCurrentUser || roleMutation.isPending}
                    title={isCurrentUser ? "Khong the doi vai tro cua chinh minh" : undefined}
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
                  <button
                    type="button"
                    className={`inline-flex h-10 items-center gap-2 rounded-full px-4 text-sm font-black text-white disabled:cursor-not-allowed disabled:opacity-60 ${banned ? "bg-emerald-600 hover:bg-emerald-700" : "bg-rose-600 hover:bg-rose-700"
                      }`}
                    disabled={isCurrentUser || statusMutation.isPending}
                    title={isCurrentUser ? "Khong the cam hoac bo cam chinh minh" : undefined}
                    onClick={() =>
                      statusMutation.mutate({
                        id: user.id,
                        status: banned ? "ACTIVE" : "PLATFORM_BANNED",
                      })
                    }
                  >
                    {banned ? <ShieldCheck className="size-4" /> : <ShieldBan className="size-4" />}
                    {banned ? "Bo cam" : "Cam"}
                  </button>
                  <button
                    type="button"
                    className="inline-flex h-10 items-center gap-2 rounded-full border border-slate-200 bg-white px-4 text-sm font-black text-slate-700 hover:border-sky-400"
                    onClick={() => setExpandedUserId(expandedUserId === user.id ? null : user.id)}
                  >
                    <Eye className="size-4" />
                    Vi pham
                  </button>
                  <span className="inline-flex h-10 items-center rounded-full bg-slate-100 px-3 text-xs font-semibold text-slate-500">
                    {user.status}
                  </span>
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
      <span className="mb-2 block text-sm font-black text-slate-700">Loc theo so dien thoai</span>
      <span className="flex h-11 items-center gap-2 rounded-full border border-slate-200 bg-white px-4 shadow-sm focus-within:border-sky-400">
        <Search className="size-4 text-slate-400" />
        <input
          value={value}
          onChange={(event) => onChange(event.target.value)}
          placeholder="Nhap so dien thoai"
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
        Dang tai trang thai vi pham...
      </div>
    );
  }

  if (query.isError) {
    return (
      <div className="mt-4 rounded-lg bg-rose-50 p-4 text-sm font-semibold text-rose-700">
        Khong the tai thong tin vi pham.
      </div>
    );
  }

  const communityViolations = query.data.community.content;
  const fieldViolations = query.data.field.content;
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
          Trang thai: {activeCount > 0 ? `${activeCount} dang hieu luc` : "Khong co vi pham dang hieu luc"}
        </span>
        <span className="text-slate-500">Tong cong {totalViolations} ban ghi vi pham</span>
      </div>
      {communityViolations.length > 0 || fieldViolations.length > 0 ? (
        <ViolationDetails communityViolations={communityViolations} fieldViolations={fieldViolations} />
      ) : (
        <p className="mt-3 text-sm text-slate-500">Chua co vi pham nao.</p>
      )}
    </div>
  );
}

function ViolationDetails({
  communityViolations,
  fieldViolations,
}: {
  communityViolations: CommunityViolation[];
  fieldViolations: FieldViolation[];
}) {
  return (
    <div className="mt-3 space-y-4 border-t border-slate-200 pt-3 text-sm text-slate-600">
      {fieldViolations.length > 0 ? (
        <section>
          <h3 className="text-xs font-black uppercase text-slate-500">Vang mat dat san</h3>
          <div className="mt-2 space-y-2 text-xs font-semibold text-slate-500">
            {fieldViolations.map((violation) => (
              <div key={violation.id} className="flex flex-wrap gap-3">
                <span>
                  San:{" "}
                  <Link
                    href={`/fields/${encodeURIComponent(violation.fieldId)}`}
                    className="font-black text-sky-700 underline-offset-2 hover:underline"
                  >
                    {violation.fieldId}
                  </Link>
                </span>
                <span>So lan: {violation.violationCount}</span>
                <span>Trang thai: {violation.banned ? "Bi cam dat san" : "Canh bao"}</span>
                {violation.lastViolationDate ? <span>Lan cuoi: {formatDateTime(violation.lastViolationDate)}</span> : null}
                {violation.banDate ? <span>Ngay cam: {formatDateTime(violation.banDate)}</span> : null}
              </div>
            ))}
          </div>
        </section>
      ) : null}
      {communityViolations.length > 0 ? (
        <section>
          <h3 className="text-xs font-black uppercase text-slate-500">Bai dang cong dong</h3>
          <div className="mt-2 space-y-2 text-xs font-semibold text-slate-500">
            {communityViolations.map((violation) => (
              <div key={violation.id} className="flex flex-wrap gap-3">
                <span>Ly do: {violation.reason}</span>
                <span>Hanh dong: {violation.action}</span>
                <span>Trang thai: {violation.status}</span>
                <span>Ngay tao: {formatDateTime(violation.createdAt)}</span>
                {violation.expireAt ? <span>Het han: {formatDateTime(violation.expireAt)}</span> : null}
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
