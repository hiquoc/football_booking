"use client";

import Link from "next/link";
import { useState } from "react";
import { ArrowUpRight } from "lucide-react";
import { BackLink } from "@/components/ui/back-link";
import { ModerationReportStatusButton } from "@/components/ui/moderation-report-status-button";
import { PageHeading } from "@/components/ui/page-heading";
import type {
  AdminModerationInput,
  CommunityModerationAction,
  CommunityReport,
  CommunityReportReason,
  CommunityReportStatus,
} from "@/lib/api/types";

import { useAdminModeration, useCommunityReports } from "@/lib/hooks/use-community";

const actionLabels: Array<[CommunityModerationAction, string]> = [
  ["NO_ACTION", "Không xử lý"],
  ["HIDE_POST", "Ẩn bài"],
  ["ISSUE_WARNING", "Cảnh báo"],
  ["TEMPORARY_POSTING_BAN", "Cấm đăng tạm thời"],
  ["PERMANENT_POSTING_BAN", "Cấm đăng vĩnh viễn"],
];

const reportReasonLabels: Record<CommunityReportReason, string> = {
  SPAM: "Spam",
  INAPPROPRIATE_CONTENT: "Nội dung không phù hợp",
  HARASSMENT: "Quấy rối",
  FAKE_INFORMATION: "Thông tin sai lệch",
  SCAM: "Lừa đảo",
  OTHER: "Khác",
};

const statusFilters: Array<[CommunityReportStatus, string]> = [
  ["PENDING", "Đang chờ"],
  ["REVIEWED", "Đã duyệt"],
];

export function CommunityModerationPanel() {
  const [page, setPage] = useState(0);
  const [status, setStatus] = useState<CommunityReportStatus>("PENDING");
  const reports = useCommunityReports(page, 20, status);
  const moderation = useAdminModeration();
  const emptyMessage =
    status === "PENDING"
      ? "Không có báo cáo đang chờ."
      : "Không có báo cáo đã duyệt.";

  return (
    <div>
      <BackLink href="/admin" className="mb-5">
        Bảng điều khiển
      </BackLink>
      <div className="flex flex-col gap-5 sm:flex-row sm:items-end sm:justify-between">
        <PageHeading
          eyebrow="Kiểm duyệt"
          title="Kiểm duyệt cộng đồng"
          description="Xem báo cáo, ẩn bài, cảnh báo và cấm đăng trong khu vực cộng đồng."
        />
        <div className="inline-flex w-fit rounded-xl border border-slate-200 bg-white p-1">
          {statusFilters.map(([value, label]) => (
            <button
              key={value}
              type="button"
              onClick={() => {
                setStatus(value);
                setPage(0);
              }}
              className={`rounded-lg px-4 py-2 text-sm font-bold ${status === value ? "bg-green-600 text-white" : "text-slate-600 hover:bg-green-50 hover:text-green-700"}`}
            >
              {label}
            </button>
          ))}
        </div>
      </div>

      {reports.data ? (
        <div className="mt-6 flex justify-between text-sm font-semibold text-slate-500">
          <span>{reports.data.totalElements} báo cáo</span>
          <span>
            Trang {reports.data.page + 1}/{Math.max(reports.data.totalPages, 1)}
          </span>
        </div>
      ) : null}

      <div className="mt-6 space-y-4">
        {reports.isPending ? <ModerationSkeleton /> : null}
        {reports.data?.empty ? (
          <p className="rounded-2xl border border-dashed border-slate-300 bg-white p-8 text-center font-bold text-slate-500">
            {emptyMessage}
          </p>
        ) : null}
        {reports.data?.content.map((report) => (
          <ReportCard
            key={report.id}
            report={report}
            onSubmit={(input) => moderation.mutate(input)}
            pending={moderation.isPending}
          />
        ))}
      </div>

      {reports.data && reports.data.totalPages > 1 ? (
        <div className="mt-6 flex items-center justify-center gap-3">
          <button
            disabled={page === 0}
            onClick={() => setPage((value) => value - 1)}
            className="rounded-xl border border-slate-200 bg-white px-4 py-2 text-sm font-bold text-slate-700 disabled:opacity-50"
          >
            Trước
          </button>
          <button
            disabled={reports.data.last}
            onClick={() => setPage((value) => value + 1)}
            className="rounded-xl bg-green-600 px-4 py-2 text-sm font-bold text-white disabled:opacity-50"
          >
            Sau
          </button>
        </div>
      ) : null}

      {moderation.error ? (
        <p className="mt-3 text-sm font-semibold text-rose-600">
          {moderation.error.message}
        </p>
      ) : null}
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
  const [reason, setReason] = useState<string>(reportReasonLabels[report.reason]);
  const [note, setNote] = useState<string>(report.description ?? "");
  const [expireAt, setExpireAt] = useState("");
  const targetUserId = report.reportedUserId ?? report.post?.ownerId;
  const reporterName = report.reporterDisplayName ?? report.reporterId;
  const reportedName = report.reportedDisplayName ?? targetUserId ?? "Người dùng";
  const postHref = `/community/${encodeURIComponent(report.postId)}`;

  return (
    <article className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
      <div className="grid gap-5 lg:grid-cols-[minmax(0,1fr)_minmax(22rem,0.58fr)]">
        <div className="min-w-0">
          <div className="flex flex-wrap items-start justify-between gap-3">
            <p className="text-xs font-black uppercase text-rose-600">
              {reportReasonLabels[report.reason]}
            </p>
            <ModerationReportStatusButton status={report.status} size="sm" />
          </div>
          <Link
            href={postHref}
            className="mt-2 block text-xl font-black text-slate-950 transition hover:text-green-700"
          >
            {report.post?.title ?? "Bài đăng"}
          </Link>
          <p className="mt-2 text-sm text-slate-600">
            {report.description ?? "Không có mô tả thêm."}
          </p>
          <div className="mt-3 flex flex-wrap gap-x-5 gap-y-2 text-sm font-semibold text-slate-500">
            <span>
              Người báo cáo:{" "}
              <Link
                href={`/users/${report.reporterId}/profile`}
                className="text-green-700 hover:text-green-800"
              >
                {reporterName}
              </Link>
            </span>
            {targetUserId ? (
              <span>
                Người bị báo cáo:{" "}
                <Link
                  href={`/users/${targetUserId}/profile`}
                  className="text-green-700 hover:text-green-800"
                >
                  {reportedName}
                </Link>
              </span>
            ) : null}
          </div>
          <Link
            href={postHref}
            className="mt-4 inline-flex items-center gap-2 text-sm font-black text-green-700 hover:text-green-800"
          >
            Xem bài đăng <ArrowUpRight className="size-4" />
          </Link>
        </div>
        <div className="space-y-3">
          <select
            value={action}
            onChange={(event) => setAction(event.target.value as CommunityModerationAction)}
            className={inputClassName}
          >
            {actionLabels.map(([value, label]) => (
              <option key={value} value={value}>
                {label}
              </option>
            ))}
          </select>
          <input
            value={reason}
            onChange={(event) => setReason(event.target.value)}
            placeholder="Lý do"
            className={inputClassName}
          />
          {action === "TEMPORARY_POSTING_BAN" ? (
            <input
              value={expireAt}
              onChange={(event) => setExpireAt(event.target.value)}
              type="datetime-local"
              className={inputClassName}
            />
          ) : null}
          <textarea
            value={note}
            onChange={(event) => setNote(event.target.value)}
            rows={2}
            placeholder="Ghi chú kiểm duyệt"
            className={inputClassName}
          />
          <button
            disabled={pending || !targetUserId}
            onClick={() =>
              onSubmit({
                action,
                targetPostId: report.postId,
                targetUserId,
                reason,
                note,
                expireAt: expireAt ? new Date(expireAt).toISOString() : undefined,
              })
            }
            className="rounded-xl bg-green-600 px-4 py-2 text-sm font-bold text-white disabled:opacity-60"
          >
            {report.status === "REVIEWED" ? "Cập nhật" : "Áp dụng"}
          </button>
        </div>
      </div>
    </article>
  );
}

function ModerationSkeleton() {
  return (
    <div className="space-y-4">
      {[0, 1, 2].map((item) => (
        <div
          key={item}
          className="h-44 animate-pulse rounded-2xl border border-slate-200 bg-white"
        />
      ))}
    </div>
  );
}

const inputClassName =
  "w-full rounded-xl border border-slate-200 px-3 py-2 text-sm outline-none focus:border-green-500 focus:ring-4 focus:ring-green-100";
