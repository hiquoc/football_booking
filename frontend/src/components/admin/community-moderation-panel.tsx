"use client";

import Link from "next/link";
import { useState } from "react";
import type {
  AdminModerationInput,
  CommunityModerationAction,
  CommunityReport,
  CommunityReportStatus,
} from "@/lib/api/types";
import { useAdminModeration, useCommunityReports } from "@/lib/hooks/use-community";

const actionLabels: Array<[CommunityModerationAction, string]> = [
  ["NO_ACTION", "Khong xu ly"],
  ["HIDE_POST", "An bai"],
  ["ISSUE_WARNING", "Canh bao"],
  ["TEMPORARY_POSTING_BAN", "Cam dang tam thoi"],
  ["PERMANENT_POSTING_BAN", "Cam dang vinh vien"],
];

const statusFilters: Array<[CommunityReportStatus, string]> = [
  ["PENDING", "Dang cho"],
  ["REVIEWED", "Da duyet"],
];

export function CommunityModerationPanel() {
  const [page, setPage] = useState(0);
  const [status, setStatus] = useState<CommunityReportStatus>("PENDING");
  const reports = useCommunityReports(page, 20, status);
  const moderation = useAdminModeration();
  const emptyMessage = status === "PENDING" ? "Khong co bao cao dang cho." : "Khong co bao cao da duyet.";

  return (
    <div>
      <div className="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <h1 className="text-3xl font-black text-slate-950">Kiem duyet cong dong</h1>
          <p className="mt-2 text-slate-600">Xem bao cao, an bai, canh bao va cam dang trong module cong dong.</p>
        </div>
        <div className="inline-flex rounded-lg border border-slate-200 bg-white p-1">
          {statusFilters.map(([value, label]) => (
            <button
              key={value}
              type="button"
              onClick={() => {
                setStatus(value);
                setPage(0);
              }}
              className={`rounded-md px-4 py-2 text-sm font-bold ${status === value ? "bg-slate-950 text-white" : "text-slate-600 hover:bg-slate-50"}`}
            >
              {label}
            </button>
          ))}
        </div>
      </div>

      {reports.data ? (
        <div className="mt-6 flex justify-between text-sm font-semibold text-slate-500">
          <span>{reports.data.totalElements} bao cao</span>
          <span>Trang {reports.data.page + 1}/{Math.max(reports.data.totalPages, 1)}</span>
        </div>
      ) : null}

      <div className="mt-6 space-y-4">
        {reports.isPending ? <p>Dang tai bao cao...</p> : null}
        {reports.data?.empty ? (
          <p className="rounded-lg border border-dashed border-slate-300 bg-white p-8 text-center font-bold text-slate-500">
            {emptyMessage}
          </p>
        ) : null}
        {reports.data?.content.map((report) => (
          <ReportCard key={report.id} report={report} onSubmit={(input) => moderation.mutate(input)} pending={moderation.isPending} />
        ))}
      </div>

      {reports.data && reports.data.totalPages > 1 ? (
        <div className="mt-6 flex items-center justify-center gap-3">
          <button disabled={page === 0} onClick={() => setPage((value) => value - 1)} className="rounded-lg border px-4 py-2 text-sm font-bold disabled:opacity-50">
            Truoc
          </button>
          <button disabled={reports.data.last} onClick={() => setPage((value) => value + 1)} className="rounded-lg border px-4 py-2 text-sm font-bold disabled:opacity-50">
            Sau
          </button>
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
  const [note, setNote] = useState<string>(report.description ?? "");
  const [expireAt, setExpireAt] = useState("");
  const targetUserId = report.reportedUserId ?? report.post?.ownerId;
  const reporterName = report.reporterDisplayName ?? report.reporterId;
  const reportedName = report.reportedDisplayName ?? targetUserId ?? "Unknown";

  return (
    <article className="rounded-lg border border-slate-200 bg-white p-5 shadow-sm">
      <div className="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
        <div>
          <p className="text-xs font-black uppercase text-rose-600">{report.reason}</p>
          <h2 className="mt-2 text-xl font-black text-slate-950">{report.post?.title ?? "Bai dang"}</h2>
          <p className="mt-2 text-sm text-slate-600">{report.description ?? "Khong co mo ta them."}</p>
          <div className="mt-3 flex flex-wrap gap-x-5 gap-y-2 text-sm font-semibold text-slate-500">
            <span>
              Nguoi bao cao:{" "}
              <Link href={`/users/${report.reporterId}/profile`} className="text-emerald-700 hover:underline">
                {reporterName}
              </Link>
            </span>
            {targetUserId ? (
              <span>
                Nguoi bi bao cao:{" "}
                <Link href={`/users/${targetUserId}/profile`} className="text-emerald-700 hover:underline">
                  {reportedName}
                </Link>
              </span>
            ) : null}
          </div>
          <p className="mt-2 text-xs font-bold uppercase text-slate-400">{report.status}</p>
        </div>
        <div className="w-full max-w-md space-y-3">
          <select value={action} onChange={(event) => setAction(event.target.value as CommunityModerationAction)} className="w-full rounded-lg border border-slate-200 px-3 py-2 text-sm">
            {actionLabels.map(([value, label]) => (
              <option key={value} value={value}>{label}</option>
            ))}
          </select>
          <input value={reason} onChange={(event) => setReason(event.target.value)} placeholder="Ly do" className="w-full rounded-lg border border-slate-200 px-3 py-2 text-sm" />
          {action === "TEMPORARY_POSTING_BAN" ? (
            <input value={expireAt} onChange={(event) => setExpireAt(event.target.value)} type="datetime-local" className="w-full rounded-lg border border-slate-200 px-3 py-2 text-sm" />
          ) : null}
          <textarea value={note} onChange={(event) => setNote(event.target.value)} rows={2} placeholder="Ghi chu kiem duyet" className="w-full rounded-lg border border-slate-200 px-3 py-2 text-sm" />
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
            {report.status === "REVIEWED" ? "Cap nhat" : "Ap dung"}
          </button>
        </div>
      </div>
    </article>
  );
}
