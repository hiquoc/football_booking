"use client";

import Link from "next/link";
import { useState } from "react";
import { Edit3, LoaderCircle, Save, X } from "lucide-react";
import type { CommunityReportReason, PublicProfile, User } from "@/lib/api/types";
import {
  useApplyCommunityPost,
  useCommunityPost,
  useCommunityPostAction,
  useDecideCommunityApplication,
  useOwnerHideCommunityPost,
  useReportCommunityPost,
  useUpdateCommunityPost,
} from "@/lib/hooks/use-community";
import { postStatusLabels, postTypeLabels, skillLabel, timeRange } from "./community-labels";
import { BackLink } from "../ui/back-link";

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
  const [reportReason, setReportReason] = useState<CommunityReportReason>("SPAM");
  const [reportDescription, setReportDescription] = useState("");
  const [hideReason, setHideReason] = useState("");

  if (isPending) return <div className="mx-auto w-full max-w-5xl p-8">Đang tải...</div>;
  if (isError || !post) return <div className="mx-auto w-full max-w-5xl p-8">Không thể tải bài đăng.</div>;

  const currentPost = post;
  const isOwner = viewer?.id === currentPost.ownerId;
  const isFieldOwner = viewer?.id === currentPost.fieldOwnerId;
  const canApply = Boolean(viewer && !isOwner && currentPost.status === "OPEN");

  function startEditing() {
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
        playersNeeded: currentPost.postType === "LOOKING_PLAYER" ? Number(playersNeeded) : undefined,
      },
      { onSuccess: () => setEditing(false) },
    );
  }

  return (
    <div className="mx-auto w-full max-w-5xl px-5 py-10 sm:px-8">
      <BackLink href="/community">Quay lại cộng đồng</BackLink>
      <article className="mt-5 rounded-lg border border-slate-200 bg-white p-6 shadow-sm">
        <div className="flex flex-wrap gap-2">
          <span className="rounded-full bg-emerald-50 px-3 py-1 text-xs font-black text-emerald-700">{postTypeLabels[post.postType]}</span>
          <span className="rounded-full bg-slate-100 px-3 py-1 text-xs font-bold text-slate-600">{postStatusLabels[post.status]}</span>
          <span className="rounded-full bg-amber-50 px-3 py-1 text-xs font-bold text-amber-700">{skillLabel(String(post.skillLevel))}</span>
        </div>
        {editing ? (
          <form onSubmit={submitUpdate} className="mt-5 grid gap-4 rounded-lg border border-slate-100 bg-slate-50 p-4">
            <input
              value={title}
              onChange={(event) => setTitle(event.target.value)}
              required
              maxLength={120}
              className="rounded-lg border border-slate-200 px-3 py-2 text-sm font-bold"
            />
            <textarea
              value={description}
              onChange={(event) => setDescription(event.target.value)}
              rows={4}
              maxLength={2000}
              className="rounded-lg border border-slate-200 px-3 py-2 text-sm"
            />
            <div className="grid gap-4 sm:grid-cols-2">
              <input
                value={skillLevel}
                onChange={(event) => setSkillLevel(event.target.value)}
                required
                maxLength={40}
                className="rounded-lg border border-slate-200 px-3 py-2 text-sm"
              />
              <input
                value={contactPhone}
                onChange={(event) => setContactPhone(event.target.value)}
                required
                pattern="^[0-9+]{9,15}$"
                className="rounded-lg border border-slate-200 px-3 py-2 text-sm"
              />
            </div>
            {post.postType === "LOOKING_PLAYER" ? (
              <input
                value={playersNeeded}
                onChange={(event) => setPlayersNeeded(event.target.value)}
                required
                min={1}
                type="number"
                className="rounded-lg border border-slate-200 px-3 py-2 text-sm"
              />
            ) : null}
            <div className="flex flex-wrap gap-2">
              <button disabled={update.isPending} className="inline-flex items-center gap-2 rounded-lg bg-emerald-600 px-4 py-2 text-sm font-bold text-white disabled:opacity-60">
                {update.isPending ? <LoaderCircle className="size-4 animate-spin" /> : <Save className="size-4" />} Lưu
              </button>
              <button type="button" onClick={() => setEditing(false)} className="inline-flex items-center gap-2 rounded-lg border border-slate-200 px-4 py-2 text-sm font-bold text-slate-700">
                <X className="size-4" /> Hủy
              </button>
            </div>
            {update.error ? <p className="text-sm font-semibold text-rose-600">{update.error.message}</p> : null}
          </form>
        ) : (
          <>
            <h1 className="mt-4 text-3xl font-black text-slate-950">{post.title}</h1>
            <p className="mt-3 whitespace-pre-line leading-7 text-slate-600">{post.description}</p>
          </>
        )}
        {post.ownerUnderModeration && post.status === "MATCHED" ? (
          <div className="mt-4 rounded-lg border border-amber-200 bg-amber-50 p-3 text-sm font-bold text-amber-900">
            Người đăng bài này đang trong diện kiểm duyệt.
          </div>
        ) : null}
        <dl className="mt-6 grid gap-4 rounded-lg bg-slate-50 p-4 text-sm sm:grid-cols-2">
          <Info label="Sân" value={post.fieldName ?? "Chưa cập nhật"} />
          <Info label="Sân con" value={post.subFieldName ?? "Chưa cập nhật"} />
          <Info label="Ngày" value={post.bookingDate} />
          <Info label="Giờ" value={timeRange(post.startTime, post.endTime)} />
          <Info label="Loại sân" value={post.fieldType ?? "Chưa cập nhật"} />
          <Info label="Contact Zalo" value={post.contactPhone} />
          {post.postType === "LOOKING_PLAYER" ? <Info label="Cầu thủ" value={`Cần ${post.playersNeeded}, đã nhận ${post.acceptedPlayersCount}`} /> : null}
        </dl>
        {post.postType === "LOOKING_OPPONENT" && post.ownerStatistics ? (
          <dl className="mt-4 grid gap-3 rounded-lg bg-emerald-50 p-4 text-sm sm:grid-cols-3">
            <Info label="Matches" value={String(post.ownerStatistics.totalMatches)} />
            <Info label="Win rate" value={formatPercent(post.ownerStatistics.winRate)} />
            <Info label="On time" value={formatPercent(post.ownerStatistics.onTimeRate)} />
            <Info label="No cancel" value={formatPercent(post.ownerStatistics.noCancelRate)} />
            <Info label="Fair play" value={formatPercent(post.ownerStatistics.fairPlayRate)} />
            <Info label="Completed bookings" value={String(post.ownerStatistics.completedBookingCount)} />
          </dl>
        ) : null}
        <div className="mt-6 flex flex-wrap gap-3">
          {!isOwner && (
            <Link href={`/users/${post.ownerId}/profile`} className="rounded-lg border border-slate-200 px-4 py-2 text-sm font-bold text-slate-700">Xem hồ sơ chủ bài</Link>
          )}
          {isOwner && post.status === "OPEN" ? (
            <>
              <button onClick={startEditing} disabled={editing} className="inline-flex items-center gap-2 rounded-lg border border-slate-200 px-4 py-2 text-sm font-bold text-slate-700 disabled:opacity-60">
                <Edit3 className="size-4" /> Sửa bài
              </button>
              <button onClick={() => action.mutate("close")} disabled={action.isPending} className="rounded-lg bg-slate-950 px-4 py-2 text-sm font-bold text-white disabled:opacity-60">Đóng bài</button>
              {post.postType === "LOOKING_PLAYER" ? (
                <button onClick={() => action.mutate("full")} disabled={action.isPending} className="rounded-lg bg-emerald-600 px-4 py-2 text-sm font-bold text-white disabled:opacity-60">Đã đủ người</button>
              ) : null}
            </>
          ) : null}
        </div>
      </article>

      {viewer && !isOwner ? (
        <section className="mt-6 grid gap-4 md:grid-cols-2">
          <div className="rounded-lg border border-slate-200 bg-white p-5">
            <h2 className="text-lg font-black text-slate-950">Báo cáo bài đăng</h2>
            <select value={reportReason} onChange={(event) => setReportReason(event.target.value as CommunityReportReason)} className="mt-3 w-full rounded-lg border border-slate-200 px-3 py-2 text-sm">
              <option value="SPAM">Spam</option>
              <option value="INAPPROPRIATE_CONTENT">Nội dung không phù hợp</option>
              <option value="HARASSMENT">Quấy rối</option>
              <option value="FAKE_INFORMATION">Thông tin giả</option>
              <option value="SCAM">Lừa đảo</option>
              <option value="OTHER">Khác</option>
            </select>
            <textarea value={reportDescription} onChange={(event) => setReportDescription(event.target.value)} rows={3} placeholder="Mô tả thêm" className="mt-3 w-full rounded-lg border border-slate-200 px-3 py-2 text-sm" />
            <button
              onClick={() => report.mutate({ reason: reportReason, description: reportDescription })}
              disabled={report.isPending}
              className="mt-3 rounded-lg border border-slate-200 px-4 py-2 text-sm font-bold text-slate-700 disabled:opacity-60"
            >
              Gửi báo cáo
            </button>
            {report.isSuccess ? <p className="mt-2 text-sm font-semibold text-emerald-700">Đã gửi báo cáo.</p> : null}
            {report.error ? <p className="mt-2 text-sm font-semibold text-rose-600">{report.error.message}</p> : null}
          </div>
          {isFieldOwner ? (
            <div className="rounded-lg border border-rose-200 bg-white p-5">
              <h2 className="text-lg font-black text-slate-950">Ẩn bài tại sân của tôi</h2>
              <textarea value={hideReason} onChange={(event) => setHideReason(event.target.value)} rows={3} placeholder="Lý do ẩn bài" className="mt-3 w-full rounded-lg border border-slate-200 px-3 py-2 text-sm" />
              <button
                onClick={() => ownerHide.mutate(hideReason)}
                disabled={ownerHide.isPending || !hideReason.trim()}
                className="mt-3 rounded-lg bg-rose-600 px-4 py-2 text-sm font-bold text-white disabled:opacity-60"
              >
                Ẩn bài
              </button>
              {ownerHide.error ? <p className="mt-2 text-sm font-semibold text-rose-600">{ownerHide.error.message}</p> : null}
            </div>
          ) : null}
        </section>
      ) : null}

      {canApply ? (
        <section className="mt-6 rounded-lg border border-slate-200 bg-white p-5">
          <h2 className="text-lg font-black text-slate-950">Ứng tuyển</h2>
          <textarea value={message} onChange={(event) => setMessage(event.target.value)} rows={3} placeholder="Lời nhắn cho chủ bài" className="mt-3 w-full rounded-lg border border-slate-200 px-3 py-2 text-sm outline-none focus:border-emerald-500" />
          <button
            disabled={apply.isPending}
            onClick={() => apply.mutate({
              message,
              applicantDisplayName: profile?.personal.fullName,
              applicantAvatarUrl: profile?.personal.avatarUrl,
              applicantTeamPhotoUrl: profile?.personal.teamPhotoUrl,
              applicantSkillLevel: profile?.personal.skillLevel,
            })}
            className="mt-3 inline-flex items-center gap-2 rounded-lg bg-emerald-600 px-4 py-2 text-sm font-bold text-white disabled:opacity-60"
          >
            {apply.isPending ? <LoaderCircle className="size-4 animate-spin" /> : null} Gửi ứng tuyển
          </button>
          {apply.error ? <p className="mt-2 text-sm font-semibold text-rose-600">{apply.error.message}</p> : null}
        </section>
      ) : null}

      {isOwner ? (
        <section className="mt-6 rounded-lg border border-slate-200 bg-white p-5">
          <h2 className="text-lg font-black text-slate-950">Ứng tuyển</h2>
          {!post.applications?.length ? <p className="mt-2 text-sm text-slate-500">Chưa có ứng tuyển.</p> : null}
          <div className="mt-4 space-y-3">
            {post.applications?.map((application) => (
              <div key={application.id} className="rounded-lg border border-slate-100 p-4">
                <div className="flex flex-wrap items-center justify-between gap-3">
                  <div>
                    <p className="font-bold text-slate-950">{application.applicantDisplayName ?? "Người chơi"}</p>
                    <p className="text-sm text-slate-500">{skillLabel(String(application.applicantSkillLevel ?? ""))} · {application.status}</p>
                  </div>
                  <Link href={`/users/${application.applicantId}/profile`} className="text-sm font-bold text-emerald-700">Xem hồ sơ</Link>
                </div>
                {application.message ? <p className="mt-2 text-sm text-slate-600">{application.message}</p> : null}
                {application.status === "PENDING" && post.status === "OPEN" ? (
                  <div className="mt-3 flex gap-2">
                    <button onClick={() => decide.mutate({ applicationId: application.id, decision: "accept" })} className="rounded-lg bg-emerald-600 px-3 py-2 text-xs font-bold text-white">Chấp nhận</button>
                    <button onClick={() => decide.mutate({ applicationId: application.id, decision: "reject" })} className="rounded-lg border border-slate-200 px-3 py-2 text-xs font-bold text-slate-700">Từ chối</button>
                  </div>
                ) : null}
              </div>
            ))}
          </div>
        </section>
      ) : null}
    </div>
  );
}

function Info({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <dt className="font-bold text-slate-500">{label}</dt>
      <dd className="mt-1 font-black text-slate-950">{value}</dd>
    </div>
  );
}

function formatPercent(value: number) {
  return `${Number(value ?? 0).toFixed(1).replace(/\.0$/, "")}%`;
}
