"use client";

import Link from "next/link";
import { useState } from "react";
import { Edit3, Flag, LoaderCircle, Save, X } from "lucide-react";
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
import { BackLink } from "../ui/back-link";
import { postStatusLabels, postTypeLabels, skillLabel, skillLevelOptions, timeRange } from "./community-labels";

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

  if (isPending) return <div className="mx-auto w-full max-w-5xl p-8">Dang tai...</div>;
  if (isError || !post) return <div className="mx-auto w-full max-w-5xl p-8">Khong the tai bai dang.</div>;

  const currentPost = post;
  const isOwner = viewer?.id === currentPost.ownerId;
  const isFieldOwner = viewer?.id === currentPost.fieldOwnerId;
  const viewerApplication = currentPost.applications?.find((application) => application.applicantId === viewer?.id);
  const matchedApplication = currentPost.applications?.find((application) =>
    application.id === currentPost.matchedApplicationId || application.status === "ACCEPTED"
  );
  const evaluationTarget = getEvaluationTarget({
    isOwner,
    post: currentPost,
    viewerApplication,
    matchedApplication,
  });
  const pendingDecision = decide.isPending ? decide.variables : null;
  const canApply = Boolean(viewer && !isOwner && currentPost.status === "OPEN" && !apply.isSuccess && !viewerApplication);
  const canReviewMatch = Boolean(
    viewer &&
    currentPost.postType === "LOOKING_OPPONENT" &&
    currentPost.status === "MATCHED" &&
    currentPost.matchResultSubmitted &&
    evaluationTarget,
  );

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
    <div className="mx-auto w-full max-w-6xl px-5 py-10 sm:px-8">
      <BackLink href="/community">Quay lai cong dong</BackLink>
      <div className="mt-5 grid gap-5 lg:grid-cols-[minmax(0,1fr)_25rem]">
        <div>
          <article className="relative rounded-lg border border-slate-200 bg-white p-6 shadow-sm">
            <div className="flex flex-wrap gap-2">
              <span className="rounded-full bg-emerald-50 px-3 py-1 text-xs font-black text-emerald-700">{postTypeLabels[post.postType]}</span>
              <span className="rounded-full bg-slate-100 px-3 py-1 text-xs font-bold text-slate-600">{postStatusLabels[post.status]}</span>
              <span className="rounded-full bg-amber-50 px-3 py-1 text-xs font-bold text-amber-700">{skillLabel(String(post.skillLevel))}</span>
            </div>

            {editing ? (
              <form onSubmit={submitUpdate} className="mt-5 grid gap-4 rounded-lg border border-slate-100 bg-slate-50 p-4">
                <input value={title} onChange={(event) => setTitle(event.target.value)} required maxLength={120} className="rounded-lg border border-slate-200 px-3 py-2 text-sm font-bold" />
                <textarea value={description} onChange={(event) => setDescription(event.target.value)} rows={4} maxLength={2000} className="rounded-lg border border-slate-200 px-3 py-2 text-sm" />
                <div className="grid gap-4 sm:grid-cols-2">
                  <select value={skillLevel} onChange={(event) => setSkillLevel(event.target.value)} required className="rounded-lg border border-slate-200 px-3 py-2 text-sm">
                    {skillLevelOptions.map(([value, label]) => <option key={value} value={value}>{label}</option>)}
                  </select>
                  <input value={contactPhone} onChange={(event) => setContactPhone(event.target.value)} required pattern="^[0-9+]{9,15}$" className="rounded-lg border border-slate-200 px-3 py-2 text-sm" />
                </div>
                {post.postType === "LOOKING_PLAYER" ? (
                  <input value={playersNeeded} onChange={(event) => setPlayersNeeded(event.target.value)} required min={1} type="number" className="rounded-lg border border-slate-200 px-3 py-2 text-sm" />
                ) : null}
                <div className="flex flex-wrap gap-2">
                  <button disabled={update.isPending} className="inline-flex items-center gap-2 rounded-lg bg-emerald-600 px-4 py-2 text-sm font-bold text-white disabled:opacity-60">
                    {update.isPending ? <LoaderCircle className="size-4 animate-spin" /> : <Save className="size-4" />} Luu
                  </button>
                  <button type="button" onClick={() => setEditing(false)} className="inline-flex items-center gap-2 rounded-lg border border-slate-200 px-4 py-2 text-sm font-bold text-slate-700">
                    <X className="size-4" /> Huy
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
                Nguoi dang bai nay dang trong dien kiem duyet.
              </div>
            ) : null}

            <dl className="mt-6 grid gap-4 rounded-lg bg-slate-50 p-4 text-sm sm:grid-cols-2">
              <Info label="San" value={post.fieldName ?? "Chua cap nhat"} />
              <Info label="San con" value={post.subFieldName ?? "Chua cap nhat"} />
              <Info label="Ngay" value={post.bookingDate} />
              <Info label="Gio" value={timeRange(post.startTime, post.endTime)} />
              <Info label="Loai san" value={post.fieldType ?? "Chua cap nhat"} />
              <Info label="Contact Zalo" value={post.contactPhone} />
              {post.postType === "LOOKING_PLAYER" ? <Info label="Cau thu" value={`Can ${post.playersNeeded}, da nhan ${post.acceptedPlayersCount}`} /> : null}
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
              {!isOwner ? (
                <Link href={`/users/${post.ownerId}/profile`} className="rounded-lg border border-slate-200 px-4 py-2 text-sm font-bold text-slate-700">Xem ho so chu bai</Link>
              ) : null}
              {isOwner && post.status === "OPEN" ? (
                <>
                  <button onClick={startEditing} disabled={editing} className="inline-flex items-center gap-2 rounded-lg border border-slate-200 px-4 py-2 text-sm font-bold text-slate-700 disabled:opacity-60">
                    <Edit3 className="size-4" /> Sua bai
                  </button>
                  <button onClick={() => action.mutate("close")} disabled={action.isPending} className="rounded-lg bg-slate-950 px-4 py-2 text-sm font-bold text-white disabled:opacity-60">Dong bai</button>
                  {post.postType === "LOOKING_PLAYER" ? (
                    <button onClick={() => action.mutate("full")} disabled={action.isPending} className="rounded-lg bg-emerald-600 px-4 py-2 text-sm font-bold text-white disabled:opacity-60">Da du nguoi</button>
                  ) : null}
                </>
              ) : null}
            </div>
            {!isOwner && (
              <button
                type="button"
                onClick={() => setReportOpen((open) => !open)}
                aria-expanded={reportOpen}
                className="absolute top-2 right-2 width-full flex items-center justify-center gap-2 rounded-lg !shadow-none bg-rose-600 px-4 py-2 text-sm font-bold text-white hover:bg-rose-500"
            >
              <Flag className="size-4" />
            </button>
            )}
          </article>
        </div>
        <aside className="space-y-3 lg:sticky lg:top-24 lg:self-start">
          {canReviewMatch && evaluationTarget ? (
            <MatchEvaluationPanel postId={post.id} target={evaluationTarget} />
          ) : null}

          {viewer && !isOwner ? (
            <>
              {canApply ? (
                <section className="rounded-lg border border-slate-200 bg-white p-5">
                  <h2 className="text-lg font-black text-slate-950">Ung tuyen</h2>
                  <textarea value={message} onChange={(event) => setMessage(event.target.value)} rows={3} placeholder="Loi nhan cho chu bai" className="mt-3 w-full rounded-lg border border-slate-200 px-3 py-2 text-sm outline-none focus:border-emerald-500" />
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
                    {apply.isPending ? <LoaderCircle className="size-4 animate-spin" /> : null} Gui ung tuyen
                  </button>
                  {apply.error ? <p className="mt-2 text-sm font-semibold text-rose-600">{apply.error.message}</p> : null}
                </section>
              ) : null}
              {apply.isSuccess ? (
                <div className="rounded-lg border border-emerald-200 bg-emerald-50 p-4 text-sm font-bold text-emerald-800">
                  Da gui ung tuyen. Vui long doi xac nhan.
                </div>
              ) : null}


              {reportOpen ? (
                <div className="rounded-lg border border-slate-200 bg-white p-4 shadow-sm">
                  <h2 className="text-base font-black text-slate-950">Bao cao bai dang</h2>
                  <select value={reportReason} onChange={(event) => setReportReason(event.target.value as CommunityReportReason)} className="mt-3 w-full rounded-lg border border-slate-200 px-3 py-2 text-sm">
                    <option value="SPAM">Spam</option>
                    <option value="INAPPROPRIATE_CONTENT">Noi dung khong phu hop</option>
                    <option value="HARASSMENT">Quay roi</option>
                    <option value="FAKE_INFORMATION">Thong tin gia</option>
                    <option value="SCAM">Lua dao</option>
                    <option value="OTHER">Khac</option>
                  </select>
                  <textarea value={reportDescription} onChange={(event) => setReportDescription(event.target.value)} rows={3} placeholder="Mo ta them" className="mt-3 w-full rounded-lg border border-slate-200 px-3 py-2 text-sm" />
                  <button
                    onClick={async () => {
                      await report.mutateAsync({ reason: reportReason, description: reportDescription });
                      setReportOpen(false);
                    }}
                    disabled={report.isPending}
                    className="mt-3 inline-flex items-center gap-2 rounded-lg border border-slate-200 px-4 py-2 text-sm font-bold text-slate-700 disabled:opacity-60"
                  >
                    {report.isPending ? <LoaderCircle className="size-4 animate-spin" /> : null} Gui bao cao
                  </button>
                </div>
              ) : null}
              {report.isSuccess ?
                <div className="rounded-lg border border-emerald-200 bg-emerald-50 p-4 text-sm font-bold text-emerald-800">
                  Da gui bao cao.
                </div> : null}
              {report.error ?
                <div className="rounded-lg border border-rose-200 bg-rose-50 p-4 text-sm font-bold text-rose-800">
                  {report.error.message}
                </div> : null}
              {isFieldOwner ? (
                <div className="rounded-lg border border-rose-200 bg-white p-4 shadow-sm">
                  <h2 className="text-base font-black text-slate-950">An bai tai san cua toi</h2>
                  <textarea value={hideReason} onChange={(event) => setHideReason(event.target.value)} rows={3} placeholder="Ly do an bai" className="mt-3 w-full rounded-lg border border-slate-200 px-3 py-2 text-sm" />
                  <button
                    onClick={() => ownerHide.mutate(hideReason)}
                    disabled={ownerHide.isPending || !hideReason.trim()}
                    className="mt-3 rounded-lg bg-rose-600 px-4 py-2 text-sm font-bold text-white disabled:opacity-60"
                  >
                    An bai
                  </button>
                  {ownerHide.error ? <p className="mt-2 text-sm font-semibold text-rose-600">{ownerHide.error.message}</p> : null}
                </div>
              ) : null}
            </>
          ) : null}
          {post.applications ? (
            <section className="rounded-lg border border-slate-200 bg-white p-5">
              <h2 className="text-lg font-black text-slate-950">Danh sach ung tuyen</h2>
              {!post.applications?.length ? <p className="mt-2 text-sm text-slate-500">Chua co ung tuyen.</p> : null}
              <div className="mt-4 space-y-3">
                {post.applications?.map((application) => {
                  const isViewerApplication = application.applicantId === viewer?.id;
                  const pendingAction = pendingDecision?.applicationId === application.id ? pendingDecision.decision : null;
                  const isDecisionLocked = Boolean(pendingDecision);

                  return (
                    <div key={application.id} className={`rounded-lg border p-4 ${pendingAction ? "border-emerald-200 bg-emerald-50/60" : "border-slate-100"}`}>
                      <div className="flex flex-wrap items-center justify-between gap-3">
                        <div>
                          <div className="flex flex-wrap items-center gap-2">
                            <p className="font-bold text-slate-950">{application.applicantDisplayName ?? "Nguoi choi"}</p>
                            {isViewerApplication ? (
                              <span className="rounded-full bg-sky-50 px-2 py-0.5 text-[11px] font-black text-sky-700">Bạn</span>
                            ) : null}
                          </div>
                          <p className="text-sm text-slate-500">{skillLabel(String(application.applicantSkillLevel ?? ""))} - {application.status}</p>
                        </div>
                        <Link href={`/users/${application.applicantId}/profile`} className="text-sm font-bold text-emerald-700">Xem ho so</Link>
                      </div>
                      {application.message ? <p className="mt-2 text-sm text-slate-600">{application.message}</p> : null}
                      {pendingAction ? (
                        <div className="mt-3 inline-flex items-center gap-2 rounded-lg border border-emerald-200 bg-white px-3 py-2 text-xs font-bold text-emerald-700">
                          <LoaderCircle className="size-4 animate-spin" />
                          {pendingAction === "accept" ? "Dang chap nhan..." : "Dang tu choi..."}
                        </div>
                      ) : null}
                      {isOwner && application.status === "PENDING" && post.status === "OPEN" ? (
                        <div className="mt-3 flex gap-2">
                          <button
                            onClick={() => decide.mutate({ applicationId: application.id, decision: "accept" })}
                            disabled={isDecisionLocked}
                            className="inline-flex items-center gap-2 rounded-lg bg-emerald-600 px-3 py-2 text-xs font-bold text-white disabled:cursor-not-allowed disabled:opacity-60"
                          >
                            {pendingAction === "accept" ? <LoaderCircle className="size-3.5 animate-spin" /> : null}
                            {pendingAction === "accept" ? "Dang chap nhan" : "Chap nhan"}
                          </button>
                          <button
                            onClick={() => decide.mutate({ applicationId: application.id, decision: "reject" })}
                            disabled={isDecisionLocked}
                            className="inline-flex items-center gap-2 rounded-lg border border-slate-200 px-3 py-2 text-xs font-bold text-slate-700 disabled:cursor-not-allowed disabled:opacity-60"
                          >
                            {pendingAction === "reject" ? <LoaderCircle className="size-3.5 animate-spin" /> : null}
                            {pendingAction === "reject" ? "Dang tu choi" : "Tu choi"}
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

function Info({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <dt className="font-bold text-slate-500">{label}</dt>
      <dd className="mt-1 font-black text-slate-950">{value}</dd>
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
      <section className="rounded-lg border border-emerald-200 bg-emerald-50 p-5 text-sm font-bold text-emerald-800">
        Da gui danh gia nguoi choi.
      </section>
    );
  }

  return (
    <section className="rounded-lg border border-slate-200 bg-white p-5 shadow-sm">
      <h2 className="text-lg font-black text-slate-950">Danh gia nguoi choi</h2>
      <p className="mt-1 text-sm text-slate-500">Danh gia {target.displayName} sau khi ket qua tran dau da duoc luu.</p>
      <div className="mt-4 grid gap-3 text-sm font-bold text-slate-700">
        <Toggle label="Den dung gio" checked={arrivedOnTime} onChange={setArrivedOnTime} />
        <Toggle label="Huy bat ngo" checked={cancelledUnexpectedly} onChange={setCancelledUnexpectedly} />
        <Toggle label="Fair play" checked={fairPlay} onChange={setFairPlay} />
        <Toggle label="Muon choi lai" checked={wouldPlayAgain} onChange={setWouldPlayAgain} />
      </div>
      <textarea
        value={comment}
        onChange={(event) => setComment(event.target.value)}
        rows={3}
        maxLength={1000}
        placeholder="Nhan xet them"
        className="mt-4 w-full rounded-lg border border-slate-200 px-3 py-2 text-sm"
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
        className="mt-3 inline-flex items-center gap-2 rounded-lg bg-emerald-600 px-4 py-2 text-sm font-bold text-white disabled:opacity-60"
      >
        {evaluation.isPending ? <LoaderCircle className="size-4 animate-spin" /> : null} Gui danh gia
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
    <label className="flex items-center justify-between gap-3 rounded-lg bg-slate-50 px-3 py-2">
      <span>{label}</span>
      <input
        type="checkbox"
        checked={checked}
        onChange={(event) => onChange(event.target.checked)}
        className="size-4 accent-emerald-600"
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
      displayName: matchedApplication.applicantDisplayName ?? "nguoi choi",
    };
  }
  if (viewerApplication?.status === "ACCEPTED") {
    return {
      userId: post.ownerId,
      displayName: post.ownerDisplayName ?? "chu bai",
    };
  }
  return null;
}

function formatPercent(value: number) {
  return `${Number(value ?? 0).toFixed(1).replace(/\.0$/, "")}%`;
}
