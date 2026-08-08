"use client";

import Link from "next/link";
import { useState } from "react";
import { Edit3, Flag, LoaderCircle, Save, X } from "lucide-react";
import { CommunityPostStatusButton } from "@/components/ui/community-post-status-button";
import { SkillLevelButton } from "@/components/ui/skill-level-button";
import { BackLink } from "@/components/ui/back-link";
import { CommunityDetailSkeleton } from "@/components/community/community-detail-skeleton";
import type { CommunityApplication, CommunityPost, CommunityReportReason, PublicProfile, User } from "@/lib/api/types";
import {
  useApplyCommunityPost,
  useCommunityPost,
  useCommunityPostAction,
  useDecideCommunityApplication,
  useOwnerHideCommunityPost,
  useReportCommunityPost,
  useSubmitMatchEvaluation,
  useUpdateCommunityPost,
} from "@/lib/hooks/use-community";
import { formatFieldType } from "@/lib/field-format";
import { postTypeLabels, skillLevelOptions, timeRange } from "./community-labels";

export function CommunityDetailContent({
  postId,
  viewer,
  profile,
}: {
  postId: string;
  viewer: User | null;
  profile: PublicProfile | null;
}) {
  const { data: post, isPending, isError } = useCommunityPost(postId);
  const apply = useApplyCommunityPost(postId);
  const action = useCommunityPostAction(postId);
  const update = useUpdateCommunityPost(postId);
  const decide = useDecideCommunityApplication(postId);
  const report = useReportCommunityPost(postId);
  const ownerHide = useOwnerHideCommunityPost(postId);

  const [editing, setEditing] = useState(false);
  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [skillLevel, setSkillLevel] = useState("");
  const [contactPhone, setContactPhone] = useState("");
  const [playersNeeded, setPlayersNeeded] = useState("");
  const [message, setMessage] = useState("");
  const [reportOpen, setReportOpen] = useState(false);
  const [reportReason, setReportReason] = useState<CommunityReportReason>("SPAM");
  const [reportDescription, setReportDescription] = useState("");
  const [hideReason, setHideReason] = useState("");

  if (isPending) return <CommunityDetailSkeleton />;
  if (isError || !post) return <div className="mx-auto w-full max-w-6xl p-8">Không thể tải bài đăng.</div>;

  const isOwner = viewer?.id === post.ownerId;
  const isFieldOwner = viewer?.id === post.fieldOwnerId;
  const viewerApplication = post.applications?.find((application) => application.applicantId === viewer?.id);
  const matchedApplication = post.applications?.find((application) =>
    application.id === post.matchedApplicationId || application.status === "ACCEPTED"
  );
  const evaluationTarget = getEvaluationTarget({
    isOwner,
    post,
    viewerApplication,
    matchedApplication,
  });
  const pendingDecision = decide.isPending ? decide.variables : null;
  const canApply = Boolean(viewer && !isOwner && post.status === "OPEN" && !apply.isSuccess && !viewerApplication);
  const canReviewMatch = Boolean(
    viewer &&
    post.postType === "LOOKING_OPPONENT" &&
    post.status === "MATCHED" &&
    post.matchResultSubmitted &&
    evaluationTarget,
  );
  const canReport = Boolean(viewer && !isOwner && !report.isSuccess);
  const hasActionControls = canReport || (isOwner && post.status === "OPEN");
  const ownerName = post.ownerDisplayName ?? "Người đăng";

  function startEditing() {
    if (!post) return;
    const currentPost = post;
    setTitle(currentPost.title);
    setDescription(currentPost.description ?? "");
    setSkillLevel(String(currentPost.skillLevel));
    setContactPhone(currentPost.contactPhone);
    setPlayersNeeded(currentPost.playersNeeded ? String(currentPost.playersNeeded) : "");
    setEditing(true);
  }

  function submitUpdate(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    update.mutate(
      {
        title: title.trim(),
        description: description.trim() || undefined,
        skillLevel: skillLevel.trim(),
        contactPhone: contactPhone.trim(),
        playersNeeded: post?.postType === "LOOKING_PLAYER" ? Number(playersNeeded) : undefined,
      },
      { onSuccess: () => setEditing(false) },
    );
  }

  return (
    <div className="mx-auto w-full max-w-[90rem] px-5 py-10 sm:px-8">
      <BackLink href="/community" className="mb-5">Quay lại cộng đồng</BackLink>

      <div className="grid gap-7 lg:grid-cols-[minmax(0,1.35fr)_minmax(24rem,0.85fr)]">
        <main className="space-y-5">
          <article className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
            <div className="flex items-start justify-between gap-3">
              <div className="flex min-w-0 flex-wrap items-center gap-2">
                <span className="inline-flex h-10 items-center rounded-lg border border-slate-200 bg-white px-3 text-sm font-black leading-none text-slate-700">
                  {postTypeLabels[post.postType]}
                </span>
                <FieldTypeBadge value={post.fieldType} />
                <SkillLevelButton value={String(post.skillLevel)} />
              </div>
              <CommunityPostStatusButton status={post.status} className="shrink-0" />
            </div>

            {editing ? (
              <form onSubmit={submitUpdate} className="mt-5 grid gap-4 rounded-2xl border border-slate-200 bg-slate-50 p-4">
                <input value={title} onChange={(event) => setTitle(event.target.value)} required maxLength={120} className={inputClassName} />
                <textarea value={description} onChange={(event) => setDescription(event.target.value)} rows={4} maxLength={2000} className={inputClassName} />
                <div className="grid gap-4 sm:grid-cols-2">
                  <select value={skillLevel} onChange={(event) => setSkillLevel(event.target.value)} required className={inputClassName}>
                    {skillLevelOptions.map(([value, label]) => <option key={value} value={value}>{label}</option>)}
                  </select>
                  <input value={contactPhone} onChange={(event) => setContactPhone(event.target.value)} required pattern="^[0-9+]{9,15}$" className={inputClassName} />
                </div>
                {post.postType === "LOOKING_PLAYER" ? (
                  <input value={playersNeeded} onChange={(event) => setPlayersNeeded(event.target.value)} required min={1} type="number" className={inputClassName} />
                ) : null}
                <div className="flex flex-wrap gap-2">
                  <button disabled={update.isPending} className="action-button min-h-11 bg-green-600 px-4 text-sm text-white disabled:opacity-60">
                    {update.isPending ? <LoaderCircle className="size-4 animate-spin" /> : <Save className="size-4" />} Lưu
                  </button>
                  <button type="button" onClick={() => setEditing(false)} className="action-button min-h-11 border border-slate-200 bg-white px-4 text-sm text-slate-700">
                    <X className="size-4" /> Hủy
                  </button>
                </div>
                {update.error ? <p className="text-sm font-semibold text-rose-600">{update.error.message}</p> : null}
              </form>
            ) : (
              <>
                <h1 className="mt-4 text-4xl font-black leading-tight text-slate-950">{post.title}</h1>
                <p className="mt-3 text-sm font-bold text-slate-500">
                  Đăng bởi{" "}
                  <Link href={`/users/${post.ownerId}/profile`} className="text-green-700 hover:text-green-800">
                    {ownerName}
                  </Link>
                </p>
                <p className="mt-3 whitespace-pre-line leading-7 text-slate-600">{post.description}</p>
              </>
            )}

            {post.ownerUnderModeration && post.status === "MATCHED" ? (
              <div className="mt-4 rounded-xl border border-amber-200 bg-amber-50 p-3 text-sm font-bold text-amber-900">
                Người đăng bài này đã vi phạm quy định cộng đồng gần đây.
              </div>
            ) : null}
          </article>

          <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
            <h2 className="text-lg font-black text-slate-950">Thông tin trận đấu</h2>
            <dl className="mt-5 grid gap-4 text-sm sm:grid-cols-2">
              <Info
                label="Sân"
                value={
                  post.fieldId && post.fieldName ? (
                    <Link href={`/fields/${post.fieldId}`} className="text-green-700 hover:text-green-800">
                      {post.fieldName}
                    </Link>
                  ) : (
                    post.fieldName ?? "Chưa cập nhật"
                  )
                }
              />
              <Info label="Sân con" value={post.subFieldName ?? "Chưa cập nhật"} />
              <Info label="Ngày" value={post.bookingDate} />
              <Info label="Giờ" value={timeRange(post.startTime, post.endTime)} />
              {/* <Info label="Loại sân" value={formatFieldType(post.fieldType)} /> */}
              <Info label="Liên hệ Zalo" value={post.contactPhone} />
              {post.postType === "LOOKING_PLAYER" ? <Info label="Cầu thủ" value={`Cần ${post.playersNeeded}, đã nhận ${post.acceptedPlayersCount}`} /> : null}
            </dl>
          </section>

          {post.postType === "LOOKING_OPPONENT" && post.ownerStatistics ? (
            <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
              <div className="flex flex-wrap items-center justify-between gap-3">
                <h2 className="text-lg font-black text-slate-950">
                  Thống kê (
                  <Link href={`/users/${post.ownerId}/profile`} className="text-green-700 hover:text-green-800">
                    {ownerName}
                  </Link>
                  )
                </h2>
                <span className="rounded-xl bg-green-600 px-3 py-2 text-sm font-black text-white">
                  {post.ownerStatistics.totalMatches} trận
                </span>
              </div>
              <div className="mt-5 grid gap-4 sm:grid-cols-2">
                <StatBar label="Tỷ lệ thắng" value={post.ownerStatistics.winRate} tone="green" />
                <StatBar label="Đúng giờ" value={post.ownerStatistics.onTimeRate} tone="amber" />
                <StatBar label="Không hủy lịch" value={post.ownerStatistics.noCancelRate} tone="slate" />
                <StatBar label="Fair play" value={post.ownerStatistics.fairPlayRate} tone="rose" />
              </div>
              <div className="mt-4 rounded-2xl border border-slate-200 bg-slate-50 p-4">
                <p className="text-sm font-bold text-slate-500">Số lần đặt sân</p>
                <p className="mt-1 text-2xl font-black text-slate-950">{post.ownerStatistics.completedBookingCount}</p>
              </div>
            </section>
          ) : null}
        </main>

        <aside className="space-y-4 lg:sticky lg:top-24 lg:self-start">
          {hasActionControls ? (
            <section className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
              <h2 className="text-xs font-black uppercase tracking-wider text-slate-500">Thao tác</h2>
              <div className="mt-4 grid gap-3">
                {isOwner && post.status === "OPEN" ? (
                  <>
                    <button onClick={startEditing} disabled={editing} className="action-button min-h-12 border border-slate-200 bg-white px-4 text-slate-700 disabled:opacity-60">
                      <Edit3 className="size-4" /> Sửa bài
                    </button>
                    <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-1">
                      <button onClick={() => action.mutate("close")} disabled={action.isPending} className="action-button min-h-12 bg-slate-900 px-4 text-white disabled:opacity-60">Đóng bài</button>
                      {post.postType === "LOOKING_PLAYER" ? (
                        <button onClick={() => action.mutate("full")} disabled={action.isPending} className="action-button min-h-12 bg-green-600 px-4 text-white disabled:opacity-60">Đã đủ người</button>
                      ) : null}
                    </div>
                  </>
                ) : null}
                {canReport ? (
                  <button
                    type="button"
                    onClick={() => setReportOpen((open) => !open)}
                    aria-expanded={reportOpen}
                    className="action-button min-h-12 border border-rose-200 bg-white px-4 text-rose-700"
                  >
                    <Flag className="size-4" />
                    Báo cáo bài đăng
                  </button>
                ) : null}
              </div>
            </section>
          ) : null}

          {canReviewMatch && evaluationTarget ? (
            <MatchEvaluationPanel postId={post.id} target={evaluationTarget} />
          ) : null}

          {viewer && !isOwner ? (
            <>
              {canApply ? (
                <section className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
                  <h2 className="text-lg font-black text-slate-950">Ứng tuyển</h2>
                  <textarea value={message} onChange={(event) => setMessage(event.target.value)} rows={3} placeholder="Lời nhắn cho chủ bài" className={`mt-3 w-full ${inputClassName}`} />
                  <button
                    disabled={apply.isPending}
                    onClick={() => apply.mutate({
                      message,
                      applicantDisplayName: profile?.personal.fullName,
                      applicantAvatarUrl: profile?.personal.avatarUrl,
                      applicantTeamPhotoUrl: profile?.personal.teamPhotoUrl,
                      applicantSkillLevel: profile?.personal.skillLevel,
                    })}
                    className="action-button mt-3 min-h-12 bg-green-600 px-4 text-sm text-white disabled:opacity-60"
                  >
                    {apply.isPending ? <LoaderCircle className="size-4 animate-spin" /> : null} Gửi ứng tuyển
                  </button>
                  {apply.error ? <p className="mt-2 text-sm font-semibold text-rose-600">{apply.error.message}</p> : null}
                </section>
              ) : null}
              {apply.isSuccess ? (
                <div className="rounded-2xl border border-green-200 bg-green-50 p-4 text-sm font-bold text-green-800">
                  Đã gửi ứng tuyển. Vui lòng đợi xác nhận.
                </div>
              ) : null}

              {reportOpen ? (
                <div className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm">
                  <h2 className="text-base font-black text-slate-950">Báo cáo bài đăng</h2>
                  <select value={reportReason} onChange={(event) => setReportReason(event.target.value as CommunityReportReason)} className={`mt-3 w-full ${inputClassName}`}>
                    <option value="SPAM">Spam</option>
                    <option value="INAPPROPRIATE_CONTENT">Nội dung không phù hợp</option>
                    <option value="HARASSMENT">Quấy rối</option>
                    <option value="FAKE_INFORMATION">Thông tin giả</option>
                    <option value="SCAM">Lừa đảo</option>
                    <option value="OTHER">Khác</option>
                  </select>
                  <textarea value={reportDescription} onChange={(event) => setReportDescription(event.target.value)} rows={3} placeholder="Mô tả thêm" className={`mt-3 w-full ${inputClassName}`} />
                  <button
                    onClick={async () => {
                      if (!window.confirm("Xác nhận gửi báo cáo bài đăng này?")) return;
                      await report.mutateAsync({ reason: reportReason, description: reportDescription });
                      setReportOpen(false);
                    }}
                    disabled={report.isPending}
                    className="action-button mt-3 min-h-12 border border-slate-200 bg-white px-4 text-sm text-slate-700 disabled:opacity-60"
                  >
                    {report.isPending ? <LoaderCircle className="size-4 animate-spin" /> : null} Gửi báo cáo
                  </button>
                </div>
              ) : null}
              {report.isSuccess ? (
                <div className="rounded-2xl border border-green-200 bg-green-50 p-4 text-sm font-bold text-green-800">
                  Đã gửi báo cáo.
                </div>
              ) : null}
              {report.error ? (
                <div className="rounded-2xl border border-rose-200 bg-rose-50 p-4 text-sm font-bold text-rose-800">
                  {report.error.message}
                </div>
              ) : null}
              {isFieldOwner ? (
                <div className="rounded-2xl border border-rose-200 bg-white p-4 shadow-sm">
                  <h2 className="text-base font-black text-slate-950">Ẩn bài tại sân của tôi</h2>
                  <textarea value={hideReason} onChange={(event) => setHideReason(event.target.value)} rows={3} placeholder="Lý do ẩn bài" className={`mt-3 w-full ${inputClassName}`} />
                  <button
                    onClick={() => ownerHide.mutate(hideReason)}
                    disabled={ownerHide.isPending || !hideReason.trim()}
                    className="action-button mt-3 min-h-12 bg-rose-500 px-4 text-sm text-white disabled:opacity-60"
                  >
                    Ẩn bài
                  </button>
                  {ownerHide.error ? <p className="mt-2 text-sm font-semibold text-rose-600">{ownerHide.error.message}</p> : null}
                </div>
              ) : null}
            </>
          ) : null}

          {post.applications ? (
            <section className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
              <h2 className="text-lg font-black text-slate-950">Danh sách ứng tuyển</h2>
              {!post.applications.length ? <p className="mt-2 text-sm text-slate-500">Chưa có ứng tuyển.</p> : null}
              <div className="mt-4 space-y-3">
                {post.applications.map((application) => {
                  const isViewerApplication = application.applicantId === viewer?.id;
                  const pendingAction = pendingDecision?.applicationId === application.id ? pendingDecision.decision : null;
                  const isDecisionLocked = Boolean(pendingDecision);

                  return (
                    <div key={application.id} className={`rounded-2xl border p-4 ${pendingAction ? "border-green-200 bg-green-50/60" : "border-slate-200"}`}>
                      <div className="flex items-start justify-between gap-3">
                        <div className="min-w-0">
                          <div className="flex flex-wrap items-center gap-2">
                            <Link href={`/users/${application.applicantId}/profile`} className="font-bold text-green-700 hover:text-green-800">
                              {application.applicantDisplayName ?? "Người chơi"}
                            </Link>
                            {isViewerApplication ? (
                              <span className="rounded-lg bg-slate-500 px-2 py-1 text-[11px] font-black text-white">Bạn</span>
                            ) : null}
                          </div>
                          <SkillLevelButton value={String(application.applicantSkillLevel ?? "")} size="sm" className="mt-2" />
                        </div>
                        <CommunityPostStatusButton status={application.status} size="sm" className="shrink-0" />
                      </div>
                      {application.message ? <p className="mt-2 text-sm text-slate-600">{application.message}</p> : null}
                      {/* {pendingAction ? (
                        <div className="mt-3 inline-flex items-center gap-2 rounded-lg border border-green-200 bg-white px-3 py-2 text-xs font-bold text-green-700">
                          <LoaderCircle className="size-4 animate-spin" />
                          {pendingAction === "accept" ? "Đang chấp nhận..." : "Đang từ chối..."}
                        </div>
                      ) : null} */}
                      {isOwner && application.status === "PENDING" && post.status === "OPEN" ? (
                        <div className="mt-3 grid gap-2 sm:grid-cols-2">
                          <button
                            onClick={() => decide.mutate({ applicationId: application.id, decision: "accept" })}
                            disabled={isDecisionLocked}
                            className="action-button min-h-10 bg-green-600 px-3 text-xs text-white disabled:cursor-not-allowed disabled:opacity-60"
                          >
                            {pendingAction === "accept" ? <LoaderCircle className="size-3.5 animate-spin" /> : null}
                            {pendingAction === "accept" ? "Đang chấp nhận" : "Chấp nhận"}
                          </button>
                          <button
                            onClick={() => decide.mutate({ applicationId: application.id, decision: "reject" })}
                            disabled={isDecisionLocked}
                            className="action-button min-h-10 border border-slate-200 bg-white px-3 text-xs text-slate-700 disabled:cursor-not-allowed disabled:opacity-60"
                          >
                            {pendingAction === "reject" ? <LoaderCircle className="size-3.5 animate-spin" /> : null}
                            {pendingAction === "reject" ? "Đang từ chối" : "Từ chối"}
                          </button>
                        </div>
                      ) : null}
                    </div>
                  );
                })}
              </div>
            </section>
          ) : null}
        </aside>
      </div>
    </div>
  );
}

const inputClassName = "rounded-xl border border-slate-200 px-3 py-2.5 text-sm outline-none focus:border-green-500 focus:ring-4 focus:ring-green-100";

function Info({ label, value }: { label: string; value: React.ReactNode }) {
  return (
    <div className="rounded-xl border border-slate-200 bg-slate-50 p-4">
      <dt className="font-bold text-slate-500">{label}</dt>
      <dd className="mt-1 font-black text-slate-950">{value}</dd>
    </div>
  );
}

function FieldTypeBadge({ value }: { value: string | null | undefined }) {
  return (
    <span className="inline-flex h-10 items-center justify-center rounded-lg border border-slate-200 bg-slate-50 px-4 text-sm font-black leading-none text-slate-700">
      {formatFieldType(value)}
    </span>
  );
}

function StatBar({
  label,
  value,
  tone,
}: {
  label: string;
  value: number;
  tone: "green" | "amber" | "slate" | "rose";
}) {
  const percentage = Math.max(0, Math.min(100, Number(value ?? 0)));
  const toneClassName = {
    green: "bg-green-600",
    amber: "bg-amber-500",
    slate: "bg-slate-600",
    rose: "bg-rose-500",
  }[tone];
  const surfaceClassName = {
    green: "bg-green-50 border-green-100",
    amber: "bg-amber-50 border-amber-100",
    slate: "bg-slate-50 border-slate-200",
    rose: "bg-rose-50 border-rose-100",
  }[tone];

  return (
    <div className={`rounded-2xl border p-4 ${surfaceClassName}`}>
      <div className="flex items-center justify-between gap-3">
        <p className="text-sm font-black text-slate-700">{label}</p>
        <strong className="text-lg font-black text-slate-950">{formatPercent(percentage)}</strong>
      </div>
      <div className="mt-3 h-2.5 overflow-hidden rounded-full bg-white ring-1 ring-slate-200">
        <div className={`h-full rounded-full ${toneClassName}`} style={{ width: `${percentage}%` }} />
      </div>
    </div>
  );
}

function MatchEvaluationPanel({
  postId,
  target,
}: {
  postId: string;
  target: { userId: string; displayName: string };
}) {
  const evaluation = useSubmitMatchEvaluation(postId);
  const [arrivedOnTime, setArrivedOnTime] = useState(true);
  const [cancelledUnexpectedly, setCancelledUnexpectedly] = useState(false);
  const [fairPlay, setFairPlay] = useState(true);
  const [wouldPlayAgain, setWouldPlayAgain] = useState(true);
  const [comment, setComment] = useState("");

  if (evaluation.isSuccess) {
    return (
      <section className="rounded-2xl border border-green-200 bg-green-50 p-5 text-sm font-bold text-green-800">
        Đã gửi đánh giá người chơi.
      </section>
    );
  }

  return (
    <section className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
      <h2 className="text-lg font-black text-slate-950">Đánh giá người chơi</h2>
      <p className="mt-1 text-sm text-slate-500">Đánh giá {target.displayName} sau khi kết quả trận đấu đã được lưu.</p>
      <div className="mt-4 grid gap-3 text-sm font-bold text-slate-700">
        <Toggle label="Đến đúng giờ" checked={arrivedOnTime} onChange={setArrivedOnTime} />
        <Toggle label="Hủy bất ngờ" checked={cancelledUnexpectedly} onChange={setCancelledUnexpectedly} />
        <Toggle label="Fair play" checked={fairPlay} onChange={setFairPlay} />
        <Toggle label="Muốn chơi lại" checked={wouldPlayAgain} onChange={setWouldPlayAgain} />
      </div>
      <textarea
        value={comment}
        onChange={(event) => setComment(event.target.value)}
        rows={3}
        maxLength={1000}
        placeholder="Nhận xét thêm"
        className={`mt-4 w-full ${inputClassName}`}
      />
      <button
        onClick={() =>
          evaluation.mutate({
            evaluatedUserId: target.userId,
            arrivedOnTime,
            cancelledUnexpectedly,
            fairPlay,
            wouldPlayAgain,
            comment: comment.trim() || undefined,
          })
        }
        disabled={evaluation.isPending}
        className="action-button mt-3 min-h-12 bg-green-600 px-4 text-sm text-white disabled:opacity-60"
      >
        {evaluation.isPending ? <LoaderCircle className="size-4 animate-spin" /> : null} Gửi đánh giá
      </button>
      {evaluation.error ? <p className="mt-2 text-sm font-semibold text-rose-600">{evaluation.error.message}</p> : null}
    </section>
  );
}

function Toggle({
  label,
  checked,
  onChange,
}: {
  label: string;
  checked: boolean;
  onChange: (checked: boolean) => void;
}) {
  return (
    <label className="flex items-center justify-between gap-3 rounded-xl border border-slate-200 bg-white px-3 py-2">
      <span>{label}</span>
      <input
        type="checkbox"
        checked={checked}
        onChange={(event) => onChange(event.target.checked)}
        className="size-4 accent-green-600"
      />
    </label>
  );
}

function getEvaluationTarget({
  isOwner,
  post,
  viewerApplication,
  matchedApplication,
}: {
  isOwner: boolean;
  post: CommunityPost | undefined;
  viewerApplication: CommunityApplication | undefined;
  matchedApplication: CommunityApplication | undefined;
}) {
  if (!post) return null;
  if (isOwner && matchedApplication) {
    return {
      userId: matchedApplication.applicantId,
      displayName: matchedApplication.applicantDisplayName ?? "người chơi",
    };
  }
  if (viewerApplication?.status === "ACCEPTED") {
    return {
      userId: post.ownerId,
      displayName: post.ownerDisplayName ?? "chủ bài",
    };
  }
  return null;
}

function formatPercent(value: number) {
  return `${Number(value ?? 0).toFixed(1).replace(/\.0$/, "")}%`;
}
