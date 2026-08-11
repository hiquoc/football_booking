"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState } from "react";
import type { ReactNode } from "react";
import { DataError, ListSkeleton } from "@/components/ui/data-state";
import type { Field } from "@/lib/api/types";
import { useModerationAuditLogs, useNoShowReports } from "@/lib/hooks/use-moderation";

type FieldOption = Pick<Field, "id" | "name">;

export function ModerationHistoryPanel({
  fields,
  selectedFieldId,
  page,
}: {
  fields: FieldOption[];
  selectedFieldId: string;
  page: number;
}) {
  const router = useRouter();
  const [currentFieldId, setCurrentFieldId] = useState(selectedFieldId);
  const [currentPage, setCurrentPage] = useState(page);
  const noShows = useNoShowReports(currentFieldId, currentPage, 20);
  const auditLogs = useModerationAuditLogs(currentFieldId, currentPage, 20);

  function selectField(fieldId: string) {
    setCurrentFieldId(fieldId);
    setCurrentPage(0);
    const params = new URLSearchParams();
    if (fieldId) params.set("fieldId", fieldId);
    router.replace(`/owner/moderation-history${params.size ? `?${params}` : ""}`, { scroll: false });
  }

  return (
    <>
      <div className="mt-6 rounded-2xl border border-slate-200 bg-white p-4">
        <label className="block text-sm font-semibold text-slate-700">
          Sân
          <select
            value={currentFieldId}
            onChange={(event) => selectField(event.target.value)}
            className="mt-2 w-full rounded-xl border border-slate-200 bg-white px-3 py-2 text-sm text-slate-900 outline-none focus:border-green-500 focus:ring-4 focus:ring-green-100"
            disabled={!fields.length}
          >
            {!fields.length ? <option value="">Chưa có sân quản lý</option> : null}
            {fields.map((field) => <option key={field.id} value={field.id}>{field.name}</option>)}
          </select>
        </label>
      </div>

      <section className="mt-6 rounded-2xl border border-slate-200 bg-white p-5">
        <h2 className="text-lg font-black text-slate-950">Báo cáo vắng mặt</h2>
        {noShows.isPending ? <ListSkeleton /> : null}
        {noShows.isError ? <DataError title="Không thể tải báo cáo vắng mặt" /> : null}
        {noShows.data ? (
          <div className="mt-4 grid gap-3">
            {noShows.data.content.map((report) => (
              <article key={report.id} className="rounded-xl border border-slate-200 bg-slate-50 p-4 text-sm">
                <div className="flex flex-wrap items-center justify-between gap-3">
                  <Link href={`/bookings/${report.bookingId}`} className="break-all font-black text-green-700 hover:text-green-800">
                    {report.bookingId}
                  </Link>
                  <span className="font-semibold text-slate-500">{formatDateTime(report.createdAt)}</span>
                </div>
                <div className="mt-2 flex flex-wrap gap-3 text-xs font-semibold text-slate-500">
                  <Link href={`/users/${report.reportedUserId}/profile`} className="text-green-700 hover:text-green-800">
                    {report.reportedUsername ?? report.reportedPhoneNumber ?? report.reportedUserId}
                  </Link>
                  {report.reportedPhoneNumber ? <span>{report.reportedPhoneNumber}</span> : null}
                </div>
              </article>
            ))}
            {!noShows.data.content.length ? <EmptyText selectedFieldId={currentFieldId} text="Chưa có báo cáo vắng mặt nào cho sân này." /> : null}
          </div>
        ) : null}
      </section>

      <section className="mt-6 rounded-2xl border border-slate-200 bg-white p-5">
        <h2 className="text-lg font-black text-slate-950">Nhật ký kiểm duyệt</h2>
        {auditLogs.isPending ? <ListSkeleton /> : null}
        {auditLogs.isError ? <DataError title="Không thể tải nhật ký kiểm duyệt" /> : null}
        {auditLogs.data ? (
          <div className="mt-4 grid gap-3">
            {auditLogs.data.content.map((log) => (
              <article key={log.id} className="rounded-xl border border-slate-200 bg-slate-50 p-4 text-sm">
                <div className="flex flex-wrap items-center justify-between gap-3">
                  <span className="font-black text-slate-950">{actionLabel(log.action)}</span>
                  <span className="font-semibold text-slate-500">{formatDateTime(log.createdAt)}</span>
                </div>
                <div className="mt-2 flex flex-wrap gap-3 text-xs font-semibold text-slate-500">
                  {log.targetUserId ? (
                    <Link href={`/users/${log.targetUserId}/profile`} className="text-green-700 hover:text-green-800">
                      {log.targetUsername ?? log.targetPhoneNumber ?? log.targetUserId}
                    </Link>
                  ) : null}
                  {log.targetPhoneNumber ? <span>{log.targetPhoneNumber}</span> : null}
                  {log.details ? <span>{log.details}</span> : null}
                </div>
              </article>
            ))}
            {!auditLogs.data.content.length ? <EmptyText selectedFieldId={currentFieldId} text="Chưa có nhật ký kiểm duyệt nào cho sân này." /> : null}
          </div>
        ) : null}
      </section>

      {auditLogs.data && auditLogs.data.totalPages > 1 ? (
        <div className="mt-6 flex items-center justify-center gap-3">
          {currentPage > 0 ? <PageLink fieldId={currentFieldId} page={currentPage - 1}>Trước</PageLink> : null}
          <span className="text-sm font-semibold text-slate-500">Trang {currentPage + 1}</span>
          {!auditLogs.data.last ? <PageLink fieldId={currentFieldId} page={currentPage + 1} primary>Sau</PageLink> : null}
        </div>
      ) : null}
    </>
  );
}

function EmptyText({ selectedFieldId, text }: { selectedFieldId: string; text: string }) {
  return (
    <p className="rounded-xl border border-dashed border-slate-300 p-6 text-center text-sm font-semibold text-slate-500">
      {selectedFieldId ? text : "Tài khoản của bạn chưa có sân để quản lý."}
    </p>
  );
}

function actionLabel(action: string) {
  const labels: Record<string, string> = {
    NO_SHOW_REPORTED: "Báo cáo vắng mặt",
    FIELD_BAN: "Cấm đặt sân",
    FIELD_UNBAN: "Gỡ cấm đặt sân",
    PLATFORM_UNBAN_RESET: "Gỡ cấm toàn hệ thống",
    PAYMENT_DISPUTE_SUBMITTED: "Gửi tranh chấp thanh toán",
    PAYMENT_DISPUTE_APPROVED: "Duyệt tranh chấp thanh toán",
    PAYMENT_DISPUTE_REJECTED: "Từ chối tranh chấp thanh toán",
  };
  return labels[action] ?? action;
}

function formatDateTime(value: string) {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return new Intl.DateTimeFormat("vi-VN", {
    dateStyle: "short",
    timeStyle: "short",
  }).format(date);
}

function PageLink({ fieldId, page, primary = false, children }: { fieldId: string; page: number; primary?: boolean; children: ReactNode }) {
  const params = new URLSearchParams({ fieldId, page: String(page) });
  return (
    <Link href={`/owner/moderation-history?${params}`} className={primary ? "rounded-xl bg-green-600 px-4 py-2 text-sm font-bold text-white" : "rounded-xl border border-slate-200 bg-white px-4 py-2 text-sm font-bold text-slate-700"}>
      {children}
    </Link>
  );
}
