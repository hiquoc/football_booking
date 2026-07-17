"use client";

import { useState } from "react";
import type { AdminModerationInput, CommunityModerationAction, CommunityReport } from "@/lib/api/types";
import { useAdminModeration, useCommunityReports } from "@/lib/hooks/use-community";

const actionLabels: Array<[CommunityModerationAction, string]> = [
  ["NO_ACTION", "Không xử lý"],
  ["HIDE_POST", "Ẩn bài"],
  ["ISSUE_WARNING", "Cảnh báo"],
  ["TEMPORARY_POSTING_BAN", "Cấm đăng tạm thời"],
  ["PERMANENT_POSTING_BAN", "Cấm đăng vĩnh viễn"],
];

export function CommunityModerationPanel() {
  const [page, setPage] = useState(0);
  const reports = useCommunityReports(page, 20, "PENDING");
  const moderation = useAdminModeration();

  return (
    <div>
      <h1 className="text-3xl font-black text-slate-950">Kiểm duyệt cộng đồng</h1>
      <p className="mt-2 text-slate-600">Xem báo cáo, ẩn bài, cảnh báo và cấm đăng trong module cộng đồng.</p>
      <div className="mt-6 space-y-4">
        {reports.isPending ? <p>Đang tải báo cáo...</p> : null}
        {reports.data?.empty ? <p className="rounded-lg border border-dashed border-slate-300 bg-white p-8 text-center font-bold text-slate-500">Không có báo cáo đang chờ.</p> : null}
        {reports.data?.content.map((report) => (
          <ReportCard key={report.id} report={report} onSubmit={(input) => moderation.mutate(input)} pending={moderation.isPending} />
        ))}
      </div>
      {reports.data && reports.data.totalPages > 1 ? (
        <div className="mt-6 flex gap-3">
          <button disabled={page === 0} onClick={() => setPage((value) => value - 1)} className="rounded-lg border px-4 py-2 text-sm font-bold disabled:opacity-50">Trước</button>
          <button disabled={reports.data.last} onClick={() => setPage((value) => value + 1)} className="rounded-lg border px-4 py-2 text-sm font-bold disabled:opacity-50">Sau</button>
        </div>
      ) : null}
      {moderation.error ? <p className="mt-3 text-sm font-semibold text-rose-600">{moderation.error.message}</p> : null}
    </div>
  );
}

function ReportCard({
  report,
  onSubmit,
  pending,
}: {
  report: CommunityReport;
  onSubmit: (input: AdminModerationInput) => void;
  pending: boolean;
}) {
  const [action, setAction] = useState<CommunityModerationAction>("HIDE_POST");
  const [reason, setReason] = useState<string>(report.reason);
  const [note, setNote] = useState("");
  const [expireAt, setExpireAt] = useState("");
  const targetUserId = report.post?.ownerId;

  return (
    <article className="rounded-lg border border-slate-200 bg-white p-5 shadow-sm">
      <div className="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
        <div>
          <p className="text-xs font-black uppercase text-rose-600">{report.reason}</p>
          <h2 className="mt-2 text-xl font-black text-slate-950">{report.post?.title ?? "Bài đăng"}</h2>
          <p className="mt-2 text-sm text-slate-600">{report.description ?? "Không có mô tả thêm."}</p>
          <p className="mt-2 text-sm font-semibold text-slate-500">Người đăng: {targetUserId}</p>
        </div>
        <div className="w-full max-w-md space-y-3">
          <select value={action} onChange={(event) => setAction(event.target.value as CommunityModerationAction)} className="w-full rounded-lg border border-slate-200 px-3 py-2 text-sm">
            {actionLabels.map(([value, label]) => <option key={value} value={value}>{label}</option>)}
          </select>
          <input value={reason} onChange={(event) => setReason(event.target.value)} placeholder="Lý do" className="w-full rounded-lg border border-slate-200 px-3 py-2 text-sm" />
          {action === "TEMPORARY_POSTING_BAN" ? (
            <input value={expireAt} onChange={(event) => setExpireAt(event.target.value)} type="datetime-local" className="w-full rounded-lg border border-slate-200 px-3 py-2 text-sm" />
          ) : null}
          <textarea value={note} onChange={(event) => setNote(event.target.value)} rows={2} placeholder="Ghi chú kiểm duyệt" className="w-full rounded-lg border border-slate-200 px-3 py-2 text-sm" />
          <button
            disabled={pending || !targetUserId}
            onClick={() => onSubmit({
              action,
              targetPostId: report.postId,
              targetUserId,
              reason,
              note,
              expireAt: expireAt ? new Date(expireAt).toISOString() : undefined,
            })}
            className="rounded-lg bg-slate-950 px-4 py-2 text-sm font-bold text-white disabled:opacity-60"
          >
            Áp dụng
          </button>
        </div>
      </div>
    </article>
  );
}
