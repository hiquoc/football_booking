"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useTransition, type FormEvent } from "react";
import { CalendarDays, ChevronLeft, ChevronRight, LoaderCircle, Search, Trophy, Users, X } from "lucide-react";
import type { CommunityPostFilters } from "@/lib/api/types";
import { useCommunityPosts } from "@/lib/hooks/use-community";
import { postStatusLabels, postTypeLabels, skillLabel, timeRange } from "./community-labels";

export function CommunityFeedContent({
  pageNumber,
  filters,
  canCreate,
}: {
  pageNumber: number;
  filters: CommunityPostFilters;
  canCreate: boolean;
}) {
  const { data, isPending, isError } = useCommunityPosts(pageNumber - 1, 10, filters);
  console.log(data?.content.at(0));

  return (
    <div className="mx-auto w-full max-w-[90rem] px-5 py-10 sm:px-8">
      <div className="mb-8 flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <p className="text-sm font-bold uppercase text-emerald-600">Cộng đồng</p>
          <h1 className="mt-2 text-4xl font-black text-slate-950">Tìm đội và cầu thủ</h1>
          <p className="mt-3 max-w-2xl text-slate-600">
            Các bài đăng đều gắn với lịch đặt sân đã xác nhận. Liên hệ nhanh qua Zalo để chốt trận.
          </p>
        </div>
        {canCreate ? (
          <Link href="/community/new" className="inline-flex items-center justify-center rounded-xl bg-emerald-600 px-5 py-3 text-sm font-black text-white hover:bg-emerald-700">
            Đăng bài mới
          </Link>
        ) : null}
      </div>
      <CommunityFilters filters={filters} />
      {isPending ? <FeedSkeleton /> : null}
      {!isPending && isError ? <State title="Không thể tải bài đăng" /> : null}
      {!isPending && data?.empty ? <State title="Chưa có bài đăng phù hợp" /> : null}
      {!isPending && data?.content.length ? (
        <>
          <div className="mb-4 flex justify-between text-sm font-semibold text-slate-500">
            <span>{data.totalElements} bài đăng</span>
            <span>Trang {data.page + 1}/{Math.max(data.totalPages, 1)}</span>
          </div>
          <div className="grid gap-5 lg:grid-cols-2">
            {data.content.map((post) => (
              <article key={post.id} className="rounded-lg border border-slate-200 bg-white p-5 shadow-sm">
                <div className="flex gap-4">
                  <div
                    className="size-14 shrink-0 rounded-full bg-emerald-100 bg-cover bg-center text-emerald-700"
                    style={post.ownerAvatarUrl ? { backgroundImage: `url(${post.ownerAvatarUrl})` } : undefined}
                  />
                  <div className="min-w-0 flex-1">
                    <div className="flex flex-wrap items-center gap-2">
                      <span className="rounded-full bg-emerald-50 px-3 py-1 text-xs font-black text-emerald-700">
                        {postTypeLabels[post.postType]}
                      </span>
                      <span className="rounded-full bg-slate-100 px-3 py-1 text-xs font-bold text-slate-600">
                        {postStatusLabels[post.status]}
                      </span>
                      <span className="rounded-full bg-amber-50 px-3 py-1 text-xs font-bold text-amber-700">
                        {skillLabel(String(post.skillLevel))}
                      </span>
                    </div>
                    <h2 className="mt-3 line-clamp-2 text-xl font-black text-slate-950">{post.title}</h2>
                    <p className="mt-2 line-clamp-2 text-sm leading-6 text-slate-600">{post.description}</p>
                  </div>
                </div>
                <div className="mt-5 grid gap-3 border-t border-slate-100 pt-4 text-sm text-slate-600 sm:grid-cols-2">
                  <span className="inline-flex items-center gap-2"><CalendarDays className="size-4" /> {post.bookingDate} · {timeRange(post.startTime, post.endTime)}</span>
                  <span>{post.fieldName} · {post.subFieldName}</span>
                  <span>Contact Zalo: <b>{post.contactPhone}</b></span>
                  {post.postType === "LOOKING_PLAYER" ? (
                    <span className="inline-flex items-center gap-2"><Users className="size-4" /> Cần {post.playersNeeded} · Đã nhận {post.acceptedPlayersCount}</span>
                  ) : (
                    <span className="inline-flex items-center gap-2"><Trophy className="size-4" /> Đang tìm đối thủ</span>
                  )}
                </div>
                {post.postType === "LOOKING_OPPONENT" && post.ownerStatistics ? (
                  <div className="mt-4 grid grid-cols-2 gap-2 rounded-lg bg-slate-50 p-3 text-xs font-bold text-slate-600 sm:grid-cols-3">
                    <span>Matches: {post.ownerStatistics.totalMatches}</span>
                    <span>Win: {formatPercent(post.ownerStatistics.winRate)}</span>
                    <span>On time: {formatPercent(post.ownerStatistics.onTimeRate)}</span>
                    <span>No cancel: {formatPercent(post.ownerStatistics.noCancelRate)}</span>
                    <span>Fair play: {formatPercent(post.ownerStatistics.fairPlayRate)}</span>
                    <span>Bookings: {post.ownerStatistics.completedBookingCount}</span>
                  </div>
                ) : null}
                <div className="mt-5 flex flex-wrap gap-3">
                  <Link href={`/community/${post.id}`} className="rounded-lg bg-slate-950 px-4 py-2 text-sm font-bold text-white hover:bg-emerald-600">
                    Xem chi tiết
                  </Link>
                  <Link href={`/users/${post.ownerId}/profile`} className="rounded-lg border border-slate-200 px-4 py-2 text-sm font-bold text-slate-700 hover:border-emerald-400">
                    Xem hồ sơ
                  </Link>
                </div>
              </article>
            ))}
          </div>
          <Pagination current={data.page + 1} total={data.totalPages} filters={filters} />
        </>
      ) : null}
    </div>
  );
}

function CommunityFilters({ filters }: { filters: CommunityPostFilters }) {
  const router = useRouter();
  const [isPending, startTransition] = useTransition();

  const submit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    const params = new URLSearchParams();
    ["postType", "skillLevel", "date", "fieldType", "district", "status", "keyword", "sortBy"].forEach((key) => {
      const value = String(form.get(key) ?? "").trim();
      if (value) params.set(key, value);
    });
    startTransition(() => router.push(`/community${params.size ? `?${params}` : ""}`));
  };

  return (
    <form onSubmit={submit} className="mb-8 rounded-lg border border-slate-200 bg-white p-4 shadow-sm">
      <div className="grid gap-3 md:grid-cols-4">
        <input name="keyword" defaultValue={filters.keyword ?? ""} placeholder="Từ khóa" className="rounded-lg border border-slate-200 px-3 py-2 text-sm font-medium outline-none focus:border-emerald-500" />
        <select name="postType" defaultValue={filters.postType ?? ""} className="rounded-lg border border-slate-200 px-3 py-2 text-sm font-medium">
          <option value="">Tất cả loại bài</option>
          <option value="LOOKING_OPPONENT">Tìm đối thủ</option>
          <option value="LOOKING_PLAYER">Tìm cầu thủ</option>
        </select>
        <input name="date" type="date" defaultValue={filters.date ?? ""} className="rounded-lg border border-slate-200 px-3 py-2 text-sm font-medium" />
        <select name="sortBy" defaultValue={filters.sortBy ?? "newest"} className="rounded-lg border border-slate-200 px-3 py-2 text-sm font-medium">
          <option value="newest">Mới nhất</option>
          <option value="upcoming">Trận gần nhất</option>
        </select>
        <input name="skillLevel" defaultValue={filters.skillLevel ?? ""} placeholder="Trình độ" className="rounded-lg border border-slate-200 px-3 py-2 text-sm font-medium" />
        <input name="fieldType" defaultValue={filters.fieldType ?? ""} placeholder="Loại sân" className="rounded-lg border border-slate-200 px-3 py-2 text-sm font-medium" />
        <input name="district" defaultValue={filters.district ?? ""} placeholder="Khu vực" className="rounded-lg border border-slate-200 px-3 py-2 text-sm font-medium" />
        <select name="status" defaultValue={filters.status ?? ""} className="rounded-lg border border-slate-200 px-3 py-2 text-sm font-medium">
          <option value="">Đang mở</option>
          <option value="MATCHED">Đã ghép đội</option>
          <option value="FULL">Đã đủ người</option>
          <option value="CLOSED">Đã đóng</option>
        </select>
      </div>
      <div className="mt-4 flex gap-3">
        <button disabled={isPending} className="inline-flex items-center gap-2 rounded-lg bg-emerald-600 px-4 py-2 text-sm font-bold text-white disabled:opacity-60">
          {isPending ? <LoaderCircle className="size-4 animate-spin" /> : <Search className="size-4" />} Lọc
        </button>
        <Link href="/community" className="inline-flex items-center gap-2 rounded-lg px-4 py-2 text-sm font-bold text-slate-500 hover:bg-slate-100">
          <X className="size-4" /> Xóa lọc
        </Link>
      </div>
    </form>
  );
}

function Pagination({ current, total, filters }: { current: number; total: number; filters: CommunityPostFilters }) {
  if (total <= 1) return null;
  return (
    <div className="mt-8 flex justify-center gap-3">
      {current > 1 ? <Link className="rounded-lg border px-4 py-2 text-sm font-bold" href={href(current - 1, filters)}><ChevronLeft className="inline size-4" /> Trước</Link> : null}
      {current < total ? <Link className="rounded-lg bg-slate-950 px-4 py-2 text-sm font-bold text-white" href={href(current + 1, filters)}>Sau <ChevronRight className="inline size-4" /></Link> : null}
    </div>
  );
}

function href(page: number, filters: CommunityPostFilters) {
  const params = new URLSearchParams();
  Object.entries(filters).forEach(([key, value]) => value && params.set(key, String(value)));
  params.set("page", String(page));
  return `/community?${params}`;
}

function State({ title }: { title: string }) {
  return <div className="rounded-lg border border-dashed border-slate-300 bg-white p-10 text-center font-bold text-slate-600">{title}</div>;
}

function FeedSkeleton() {
  return <div className="grid gap-5 lg:grid-cols-2">{Array.from({ length: 4 }, (_, i) => <div key={i} className="h-64 animate-pulse rounded-lg bg-slate-200" />)}</div>;
}

function formatPercent(value: number) {
  return `${Number(value ?? 0).toFixed(1).replace(/\.0$/, "")}%`;
}
