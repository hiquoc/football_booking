"use client";

import Link from "next/link";
import { AlertTriangle, Ban, CheckCircle, X } from "lucide-react";
import { useRouter } from "next/navigation";
import { useState, type ReactNode } from "react";
import { SearchableFieldSelect } from "@/components/admin/searchable-field-select";
import { PaymentDisputeStatusButton } from "@/components/ui/payment-dispute-status-button";
import { DataError, ListSkeleton } from "@/components/ui/data-state";
import type { FieldSearchOption, PaymentDisputeReport, PaymentDisputeStatus } from "@/lib/api/types";
import { useAdminPaymentDisputes, useAdminPlayerBan } from "@/lib/hooks/use-moderation";
import { BookingStatusButton } from "../bookings/booking-status-button";

const statusOptions: Array<{ value?: PaymentDisputeStatus; label: string }> = [
  { label: "Tất cả" },
  { value: "PENDING", label: "Chờ xử lý" },
  { value: "APPROVED", label: "Đã chấp nhận" },
  { value: "REJECTED", label: "Đã từ chối" },
];

export function PaymentDisputesPanel({
  page,
  status,
  fieldIds = [],
}: {
  page: number;
  status?: PaymentDisputeStatus;
  fieldIds?: string[];
}) {
  const router = useRouter();
  const [currentStatus, setCurrentStatus] = useState<PaymentDisputeStatus | undefined>(status);
  const [currentFieldIds, setCurrentFieldIds] = useState<string[]>(fieldIds);
  const [selectedFields, setSelectedFields] = useState<FieldSearchOption[]>([]);
  const [currentPage, setCurrentPage] = useState(page);
  const filters = { status: currentStatus, fieldIds: currentFieldIds };
  const query = useAdminPaymentDisputes(currentPage, 20, filters);

  function updateFilters(next: { status?: PaymentDisputeStatus; fieldIds?: string[] }, nextSelectedFields = selectedFields) {
    const nextFieldIds = next.fieldIds ?? [];
    setCurrentStatus(next.status);
    setCurrentFieldIds(nextFieldIds);
    setSelectedFields(nextSelectedFields);
    setCurrentPage(0);
    const params = new URLSearchParams();
    if (next.status) params.set("status", next.status);
    if (nextFieldIds.length) params.set("fieldIds", nextFieldIds.join(","));
    router.replace(`/admin/payment-disputes${params.size ? `?${params}` : ""}`, { scroll: false });
  }

  function updatePage(nextPage: number) {
    setCurrentPage(nextPage);
    const params = new URLSearchParams({ page: String(nextPage + 1) });
    if (currentStatus) params.set("status", currentStatus);
    if (currentFieldIds.length) params.set("fieldIds", currentFieldIds.join(","));
    router.replace(`/admin/payment-disputes?${params}`, { scroll: false });
  }

  return (
    <section className="mt-8">
      <div className="grid gap-3 rounded-2xl border border-slate-200 bg-white p-4 md:grid-cols-[1fr_1fr]">
        <label className="text-sm font-semibold text-slate-700">
          Trạng thái
          <select
            value={currentStatus ?? ""}
            onChange={(event) => updateFilters({ status: parseStatus(event.target.value), fieldIds: currentFieldIds })}
            className="mt-2 min-h-11 w-full rounded-xl border border-slate-200 bg-slate-50 px-3 py-2 text-sm font-semibold text-slate-700 outline-none focus:border-green-500 focus:bg-white focus:ring-4 focus:ring-green-100"
          >
            {statusOptions.map((option) => <option key={option.value ?? "all"} value={option.value ?? ""}>{option.label}</option>)}
          </select>
        </label>
        <label className="text-sm font-semibold text-slate-700">
          Sân
          <div className="mt-2">
            <SearchableFieldSelect
              value={currentFieldIds}
              selectedFields={selectedFields}
              onChange={(nextFieldIds, nextSelectedFields) =>
                updateFilters({ status: currentStatus, fieldIds: nextFieldIds }, nextSelectedFields)
              }
            />
          </div>
        </label>
      </div>

      {query.isPending ? <div className="mt-6"><ListSkeleton /></div> : null}
      {query.isError ? <div className="mt-6"><DataError title="Không thể tải tranh chấp thanh toán" /></div> : null}
      {query.data ? <PaymentDisputeList reports={query.data.content} /> : null}
      {query.data && query.data.totalPages > 1 ? (
        <div className="mt-6 flex items-center justify-center gap-3">
          {currentPage > 0 ? <PageButton onClick={() => updatePage(currentPage - 1)}>Trước</PageButton> : null}
          <span className="text-sm font-semibold text-slate-500">Trang {query.data.page + 1}/{Math.max(query.data.totalPages, 1)}</span>
          {!query.data.last ? <PageButton onClick={() => updatePage(currentPage + 1)} primary>Sau</PageButton> : null}
        </div>
      ) : null}
    </section>
  );
}

function PaymentDisputeList({ reports }: { reports: PaymentDisputeReport[] }) {
  if (!reports.length) {
    return (
      <div className="mt-6 rounded-2xl border border-dashed border-slate-300 bg-white p-10 text-center text-sm text-slate-500">
        Chưa có tranh chấp nào phù hợp.
      </div>
    );
  }

  return (
    <div className="mt-6 grid gap-4">
      {reports.map((report) => <PaymentDisputeCard key={report.id} report={report} />)}
    </div>
  );
}

function PaymentDisputeCard({ report }: { report: PaymentDisputeReport }) {
  const banned = isPlatformBanned(report.reportedUserStatus);
  const profileLabel = report.reportedUsername?.trim() || report.reportedPhoneNumber?.trim() || report.reportedUserId;

  return (
    <article className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
      <div className="flex flex-col justify-between gap-3 sm:flex-row sm:items-start">
        <div>
          <p className="text-xs font-black uppercase text-slate-400">Mã báo cáo</p>
          <h2 className="mt-1 break-all text-lg font-black text-slate-950">{report.id}</h2>
        </div>
        <PaymentDisputeStatusButton status={report.status} size="sm" />
      </div>

      <div className="mt-4 grid gap-4 lg:grid-cols-[1.2fr_1fr]">
        <div>
          <p className="text-sm leading-6 text-slate-600">{report.description}</p>
          {report.adminNote ? (
            <p className="mt-3 rounded-xl bg-slate-50 p-3 text-sm font-semibold text-slate-600">
              Ghi chú quản trị: {report.adminNote}
            </p>
          ) : null}
        </div>
        <div className="rounded-xl border border-slate-100 bg-slate-50 p-4 text-sm text-slate-600">
          <p className="font-black text-slate-950">Người bị báo cáo</p>
          <Link href={`/users/${report.reportedUserId}/profile`} className="mt-2 block break-all font-black text-green-700 hover:text-green-800">
            {profileLabel}
          </Link>
          {report.reportedPhoneNumber ? <p className="mt-1 font-semibold text-slate-500">{report.reportedPhoneNumber}</p> : null}
          
          <div className="mt-3">
            <AdminPlayerBanButton userId={report.reportedUserId} banned={banned} />
          </div>
        </div>
      </div>

      <div className="mt-4 grid gap-2 border-t border-slate-100 pt-4 text-sm text-slate-500 md:grid-cols-3">
        <span>
          <span className="font-bold text-slate-500">Mã đặt sân: </span>
          <strong className="text-slate-900">{report.bookingCode ?? report.bookingId}</strong>
        </span>
        <Reference label="Sân" href={`/fields/${report.fieldId}`} value={report.fieldName ?? report.fieldId} />
        <span>
          <span className="font-bold text-slate-500">Khung giờ: </span>
          <strong className="text-slate-900">{bookingTime(report)}</strong>
        </span>
        {report.subFieldName || report.subFieldId ? (
          <span>
            <span className="font-bold text-slate-500">Sân con: </span>
            <strong className="text-slate-900">{report.subFieldName ?? report.subFieldId ?? ""}</strong>
          </span>
        ) : null}
        {report.bookingPrice != null ? (
          <span>
            <span className="font-bold text-slate-500">Giá: </span>
            <strong className="text-slate-900">{formatMoney(report.bookingPrice)}</strong>
          </span>
        ) : null}
        {report.bookingStatus ? (
          <span>
            <span className="font-bold text-slate-500">Trạng thái đặt sân: </span>
            <BookingStatusButton status={report.bookingStatus} size="sm" />
          </span>
        ) : null}
      </div>
    </article>
  );
}

function AdminPlayerBanButton({ userId, banned }: { userId: string; banned: boolean }) {
  const [confirmOpen, setConfirmOpen] = useState(false);
  const mutation = useAdminPlayerBan();
  const currentBanned = mutation.data ? isPlatformBanned(mutation.data.status) || Boolean(mutation.data.isPermanentBan) : banned;

  function submit() {
    mutation.mutate(
      { userId, banned: !currentBanned },
      {
        onSuccess: () => setConfirmOpen(false),
      },
    );
  }

  return (
    <div className="flex flex-wrap items-center gap-2">
      <span className={`inline-flex h-9 items-center rounded-lg px-3 text-xs font-black text-white ${currentBanned ? "bg-slate-500" : "bg-green-600"}`}>
        {currentBanned ? "Đã cấm" : "Đang hoạt động"}
      </span>
      <button
        type="button"
        onClick={() => setConfirmOpen(true)}
        disabled={mutation.isPending}
        className={`inline-flex h-9 items-center gap-2 rounded-lg px-3 text-xs font-black text-white disabled:opacity-60 ${currentBanned ? "bg-green-600 hover:bg-green-700" : "bg-rose-600 hover:bg-rose-700"}`}
      >
        {currentBanned ? <CheckCircle className="size-4" /> : <Ban className="size-4" />}
        {currentBanned ? "Bỏ cấm" : "Cấm"}
      </button>
      {mutation.error ? <p className="basis-full text-xs font-semibold text-rose-600">{mutation.error.message}</p> : null}
      {confirmOpen ? (
        <ConfirmPanel
          banned={currentBanned}
          pending={mutation.isPending}
          onClose={() => setConfirmOpen(false)}
          onConfirm={submit}
        />
      ) : null}
    </div>
  );
}

function ConfirmPanel({ banned, pending, onClose, onConfirm }: { banned: boolean; pending: boolean; onClose: () => void; onConfirm: () => void }) {
  return (
    <div className="fixed inset-0 z-[90] grid place-items-center bg-slate-950/55 p-4" role="dialog" aria-modal="true" aria-labelledby="admin-player-ban-title">
      <form
        onSubmit={(event) => {
          event.preventDefault();
          onConfirm();
        }}
        className="w-full max-w-lg rounded-2xl border border-slate-200 bg-white p-5 shadow-2xl shadow-slate-950/20"
      >
        <div className="flex items-start justify-between gap-4">
          <div>
            <p className="inline-flex items-center gap-2 text-xs font-black uppercase text-rose-600">
              <AlertTriangle className="size-4" />
              Xác nhận thao tác
            </p>
            <h3 id="admin-player-ban-title" className="mt-2 text-lg font-black text-slate-950">
              {banned ? "Bỏ cấm người chơi" : "Cấm người chơi"}
            </h3>
            <p className="mt-2 text-sm font-semibold text-slate-600">
              {banned ? "Người dùng sẽ được phép sử dụng tài khoản trở lại." : "Người dùng sẽ bị cấm đặt sân và sử dụng các tính năng liên quan."}
            </p>
          </div>
          <button type="button" onClick={onClose} disabled={pending} className="rounded-full p-2 text-slate-500 hover:bg-slate-100 hover:text-slate-900 disabled:opacity-50" aria-label="Đóng">
            <X className="size-5" />
          </button>
        </div>
        <div className="mt-5 flex flex-col-reverse gap-3 sm:flex-row sm:justify-end">
          <button type="button" onClick={onClose} disabled={pending} className="rounded-xl border border-slate-200 px-4 py-2.5 text-sm font-black text-slate-700 hover:bg-slate-50 disabled:opacity-50">
            Hủy
          </button>
          <button type="submit" disabled={pending} className="rounded-xl bg-rose-500 px-4 py-2.5 text-sm font-black text-white hover:bg-rose-600 disabled:opacity-60">
            {pending ? "Đang xử lý..." : banned ? "Xác nhận bỏ cấm" : "Xác nhận cấm"}
          </button>
        </div>
      </form>
    </div>
  );
}

function Reference({ label, href, value }: { label: string; href: string; value: string }) {
  return (
    <span className="min-w-0">
      <span className="font-bold text-slate-500">{label}: </span>
      <Link href={href} className="break-all font-black text-green-700 hover:text-green-800">{value}</Link>
    </span>
  );
}

function PageButton({ onClick, primary = false, children }: { onClick: () => void; primary?: boolean; children: ReactNode }) {
  return (
    <button type="button" onClick={onClick} className={primary ? "rounded-xl bg-green-600 px-4 py-2 text-sm font-bold text-white" : "rounded-xl border border-slate-200 bg-white px-4 py-2 text-sm font-bold text-slate-700"}>
      {children}
    </button>
  );
}

function parseStatus(value: string): PaymentDisputeStatus | undefined {
  return value === "PENDING" || value === "APPROVED" || value === "REJECTED" ? value : undefined;
}

function bookingTime(report: PaymentDisputeReport) {
  const date = report.bookingDate ?? report.startDateTime?.slice(0, 10);
  const start = report.startTime ?? report.startDateTime?.slice(11, 16);
  const end = report.endTime ?? report.endDateTime?.slice(11, 16);
  return [date, start && end ? `${start}-${end}` : start].filter(Boolean).join(" ");
}

function formatMoney(value: number) {
  return new Intl.NumberFormat("vi-VN", { style: "currency", currency: "VND", maximumFractionDigits: 0 }).format(value);
}

function isPlatformBanned(status?: string | null) {
  return status === "PLATFORM_BANNED" || status === "PERMANENT";
}

function userStatusLabel(status?: string | null) {
  if (isPlatformBanned(status)) return "Đã cấm";
  if (status === "ACTIVE") return "Đang hoạt động";
  return "Không rõ";
}
