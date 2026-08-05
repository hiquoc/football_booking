"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useMemo, useState, useTransition } from "react";
import { Provinces } from "vietnam-divisions-js";
import type { District } from "vietnam-divisions-js/districts";
import type { Province } from "vietnam-divisions-js/provinces";
import { CalendarDays, ChevronLeft, ChevronRight, LoaderCircle, MapPin, Trophy, Users } from "lucide-react";
import { CommunityPostStatusButton } from "@/components/ui/community-post-status-button";
import { SkillLevelButton } from "@/components/ui/skill-level-button";
import { BackLink } from "@/components/ui/back-link";
import type { CommunityPostFilters } from "@/lib/api/types";
import { communityFieldTypeOptions, formatFieldType } from "@/lib/field-format";
import { useCommunityPosts } from "@/lib/hooks/use-community";
import { useFieldCards } from "@/lib/hooks/use-fields";
import { postTypeLabels, skillLevelOptions, timeRange } from "./community-labels";

const postTypes = [
  ["LOOKING_OPPONENT", "Tìm đối thủ"],
  ["LOOKING_PLAYER", "Tìm thêm cầu thủ"],
] as const;

const statuses = [
  ["all", "Tất cả"],
  ["OPEN", "Đang mở"],
  ["MATCHED", "Đã ghép đội"],
  ["FULL", "Đã đủ người"],
  ["CLOSED", "Đã đóng"],
  ["CANCELLED", "Đã hủy"],
] as const;

export function CommunityFeedContent({
  pageNumber,
  filters,
  viewerId,
  canCreate,
  basePath = "/community",
}: {
  pageNumber: number;
  filters: CommunityPostFilters;
  viewerId: string | null;
  canCreate: boolean;
  basePath?: string;
}) {
  const sortedFilters = { ...filters, sortBy: filters.sortBy ?? "desc" } satisfies CommunityPostFilters;
  const { data, isPending, isError } = useCommunityPosts(pageNumber - 1, 10, sortedFilters);
  const pageTitle = basePath === "/community/my-posts"
    ? "Bài đăng của tôi"
    : basePath === "/community/my-applications"
      ? "Bài đã ứng tuyển"
      : "Tìm đội và cầu thủ";

  return (
    <div className="mx-auto w-full max-w-[90rem] px-5 py-10 sm:px-8">
      <BackLink href={basePath === "/community" ? "/" : "/community"} className="mb-5">
        {basePath === "/community" ? "Quay lại trang chủ" : "Quay lại cộng đồng"}
      </BackLink>

      <header className="mb-6 rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
        <div className="flex flex-col gap-5 lg:flex-row lg:items-end lg:justify-between">
          <div>
            <p className="text-sm font-bold uppercase tracking-wider text-green-600">Cộng đồng</p>
            <h1 className="mt-2 text-4xl font-black text-slate-950">{pageTitle}</h1>
            <p className="mt-3 max-w-2xl text-slate-600">
              Các bài đăng gắn với lịch đặt sân đã xác nhận, sắp xếp theo ngày và giờ thi đấu.
            </p>
          </div>
          {canCreate ? (
            <Link
              href="/community/new"
              className="action-button w-full bg-green-600 px-5 text-white hover:bg-green-700 sm:w-fit"
            >
              Đăng bài mới
            </Link>
          ) : null}
        </div>

        {viewerId ? (
          <nav className="mt-5 flex flex-wrap gap-2 border-t border-slate-200 pt-5">
            <Link href="/community?sortBy=upcoming" className={navClass(basePath === "/community")}>
              Tất cả bài đăng
            </Link>
            <Link href="/community/my-posts?status=all&sortBy=newest" className={navClass(basePath === "/community/my-posts")}>
              Bài của tôi
            </Link>
            <Link href="/community/my-applications?status=all&sortBy=newest" className={navClass(basePath === "/community/my-applications")}>
              Đã ứng tuyển
            </Link>
          </nav>
        ) : null}
      </header>

      <CommunityFilters filters={sortedFilters} basePath={basePath} />

      {isPending ? <FeedSkeleton /> : null}
      {!isPending && isError ? <State title="Không thể tải bài đăng" /> : null}
      {!isPending && data?.empty ? <State title="Chưa có bài đăng phù hợp" /> : null}
      {!isPending && data?.content.length ? (
        <>
          <div className="mb-4 flex justify-between text-sm font-semibold text-slate-500">
            <span>{data.totalElements} bài đăng</span>
            <span>Trang {data.page + 1}/{Math.max(data.totalPages, 1)}</span>
          </div>
          <div className="grid gap-5 xl:grid-cols-2">
            {data.content.map((post) => (
              <article
                key={post.id}
                className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm transition hover:border-green-300 hover:shadow-[0_16px_36px_rgba(15,23,42,0.08)]"
              >
                <div className="flex items-start gap-4">
                  <div
                    className="flex size-14 shrink-0 items-center justify-center rounded-full border border-slate-200 bg-green-100 bg-cover bg-center font-black text-lg text-green-700 ring-1 ring-green-200"
                    style={
                      post.ownerAvatarUrl
                        ? { backgroundImage: `url(${post.ownerAvatarUrl})` }
                        : undefined
                    }
                  >
                    {post.ownerAvatarUrl
                      ? null
                      : (post.ownerDisplayName || post.contactPhone)
                        .slice(0, 1)
                        .toUpperCase()}
                  </div>
                  <div className="min-w-0 flex-1">
                    <div className="flex items-start justify-between gap-3">
                      <div className="flex min-w-0 flex-wrap items-center gap-2">
                        <span className="inline-flex h-8 items-center rounded-lg border border-slate-200 bg-white px-3 text-xs font-black leading-none text-slate-700">
                          {postTypeLabels[post.postType]}
                        </span>

                        <FieldTypeBadge value={post.fieldType} size="sm" />
                        <SkillLevelButton value={String(post.skillLevel)} size="sm" />

                      </div>
                      <CommunityPostStatusButton status={post.status} size="sm" className="shrink-0" />
                    </div>
                    <h2 className="mt-3 line-clamp-2 text-xl font-black text-slate-950">{post.title}</h2>
                    <p className="mt-1 text-sm font-bold text-slate-500">
                      Đăng bởi{" "}
                      <Link href={`/users/${post.ownerId}/profile`} className="text-green-700 hover:text-green-800">
                        {post.ownerDisplayName ?? "Người đăng"}
                      </Link>
                    </p>
                    <p className="mt-2 line-clamp-2 text-sm leading-6 text-slate-600">{post.description}</p>
                  </div>
                </div>

                <div className="mt-5 grid gap-3 border-t border-slate-200 pt-4 text-sm text-slate-600 sm:grid-cols-2">
                  <span className="inline-flex items-center gap-2">
                    <CalendarDays className="size-4 text-green-600" />
                    {post.bookingDate} · {timeRange(post.startTime, post.endTime)}
                  </span>
                  <span className="inline-flex items-center gap-2">
                    <MapPin className="size-4 text-green-600" />
                    {post.fieldName} · {post.subFieldName}
                  </span>
                  <span>Liên hệ Zalo: <b>{post.contactPhone}</b></span>
                  {post.postType === "LOOKING_PLAYER" ? (
                    <span className="inline-flex items-center gap-2">
                      <Users className="size-4 text-green-600" />
                      Cần {post.playersNeeded} · Đã nhận {post.acceptedPlayersCount}
                    </span>
                  ) : (
                    <span className="inline-flex items-center gap-2">
                      <Trophy className="size-4 text-green-600" />
                      Đang tìm đối thủ
                    </span>
                  )}
                </div>

                {post.postType === "LOOKING_OPPONENT" && post.ownerStatistics ? (
                  <div className="mt-4 grid grid-cols-2 gap-2 rounded-xl border border-slate-200 bg-slate-50 p-3 text-xs font-bold text-slate-600 sm:grid-cols-3">
                    <span>Trận: {post.ownerStatistics.totalMatches}</span>
                    <span>Thắng: {formatPercent(post.ownerStatistics.winRate)}</span>
                    <span>Đúng giờ: {formatPercent(post.ownerStatistics.onTimeRate)}</span>
                    <span>Không hủy: {formatPercent(post.ownerStatistics.noCancelRate)}</span>
                    <span>Fair play: {formatPercent(post.ownerStatistics.fairPlayRate)}</span>
                    <span>Lịch xong: {post.ownerStatistics.completedBookingCount}</span>
                  </div>
                ) : null}

                <div className="mt-5 flex flex-wrap justify-end gap-3">
                  <Link href={`/community/${post.id}`} className="rounded-xl bg-green-600 px-4 py-2.5 text-sm font-black text-white hover:bg-green-700">
                    Xem chi tiết
                  </Link>
                </div>
              </article>
            ))}
          </div>
          <Pagination current={data.page + 1} total={data.totalPages} filters={sortedFilters} basePath={basePath} />
        </>
      ) : null}
    </div>
  );
}

function CommunityFilters({ filters, basePath }: { filters: CommunityPostFilters; basePath: string }) {
  const router = useRouter();
  const [isPending, startTransition] = useTransition();
  const [provinces, setProvinces] = useState<Province[]>([]);
  const [districts, setDistricts] = useState<District[]>([]);
  const [values, setValues] = useState({
    keyword: filters.keyword ?? "",
    postType: filters.postType ?? "",
    date: filters.date ?? "",
    skillLevel: filters.skillLevel ?? "",
    fieldType: filters.fieldType ?? "",
    city: filters.city ?? "",
    district: filters.district ?? "",
    fieldName: filters.fieldName ?? "",
    status: filters.status ?? "all",
    sortBy: filters.sortBy ?? "desc",
  });
  const [debouncedFieldName, setDebouncedFieldName] = useState(values.fieldName);
  const selectedProvinceCode = useMemo(
    () => provinces.find((province) => province.name === values.city)?.idProvince ?? "",
    [provinces, values.city],
  );
  const fieldSuggestions = useFieldCards(0, 8, debouncedFieldName.trim() ? { keyword: debouncedFieldName.trim() } : {});
  const showFieldSuggestions = values.fieldName.trim().length > 0 && Boolean(fieldSuggestions.data?.content.length);

  useEffect(() => {
    let active = true;
    Provinces.getAllProvincesSorted().then((items) => {
      if (active) setProvinces(items);
    });
    return () => {
      active = false;
    };
  }, []);

  useEffect(() => {
    let active = true;
    if (!selectedProvinceCode) {
      Promise.resolve().then(() => {
        if (active) setDistricts([]);
      });
      return () => {
        active = false;
      };
    }
    Provinces.getDistrictsByProvinceId(selectedProvinceCode).then((items) => {
      if (active) setDistricts(items);
    });
    return () => {
      active = false;
    };
  }, [selectedProvinceCode]);

  useEffect(() => {
    const timer = window.setTimeout(() => setDebouncedFieldName(values.fieldName), 400);
    return () => window.clearTimeout(timer);
  }, [values.fieldName]);

  useEffect(() => {
    const timer = window.setTimeout(() => {
      const params = new URLSearchParams();
      Object.entries(values).forEach(([key, value]) => {
        const nextValue = value.trim();
        if (nextValue) params.set(key, nextValue);
      });
      startTransition(() => router.push(params.size ? `${basePath}?${params}` : basePath));
    }, 300);
    return () => window.clearTimeout(timer);
  }, [basePath, router, values]);

  const update = (key: keyof typeof values, value: string) => {
    setValues((current) => ({
      ...current,
      [key]: value,
      ...(key === "city" ? { district: "" } : null),
    }));
  };

  return (
    <div className="mb-8 rounded-2xl border border-slate-200 bg-white p-4 shadow-sm">
      <div className="grid gap-3 md:grid-cols-4">
        <input value={values.keyword} onChange={(event) => update("keyword", event.target.value)} placeholder="Từ khóa" className={inputClassName} />
        <Select value={values.postType} onChange={(value) => update("postType", value)} options={postTypes} placeholder="Tất cả loại bài" />
        <input value={values.date} onChange={(event) => update("date", event.target.value)} type="date" className={inputClassName} />
        <Select value={values.skillLevel} onChange={(value) => update("skillLevel", value)} options={skillLevelOptions} placeholder="Tất cả trình độ" />
        <Select value={values.fieldType} onChange={(value) => update("fieldType", value)} options={communityFieldTypeOptions} placeholder="Tất cả loại sân" />
        <select value={values.city} onChange={(event) => update("city", event.target.value)} className={inputClassName}>
          <option value="">Tất cả tỉnh/thành</option>
          {provinces.map((province) => (
            <option key={province.idProvince} value={province.name}>{province.name}</option>
          ))}
        </select>
        <select value={values.district} onChange={(event) => update("district", event.target.value)} disabled={!values.city} className={`${inputClassName} disabled:bg-slate-100 disabled:text-slate-400`}>
          <option value="">Tất cả quận/huyện</option>
          {districts.map((district) => (
            <option key={district.idDistrict} value={district.name}>{district.name}</option>
          ))}
        </select>
        <div className="relative">
          <input value={values.fieldName} onChange={(event) => update("fieldName", event.target.value)} placeholder="Tên sân" className={`w-full ${inputClassName}`} />
          {showFieldSuggestions ? (
            <div className="absolute z-20 mt-1 max-h-60 w-full overflow-auto rounded-xl border border-slate-200 bg-white py-1 text-sm shadow-lg">
              {fieldSuggestions.data?.content.map((field) => (
                <button
                  key={field.id}
                  type="button"
                  onClick={() => update("fieldName", field.name)}
                  className="block w-full px-3 py-2 text-left font-medium text-slate-700 hover:bg-green-50"
                >
                  {field.name}
                </button>
              ))}
            </div>
          ) : null}
        </div>
        <Select value={values.status} onChange={(value) => update("status", value)} options={statuses} />
      </div>
      {isPending ? <p className="mt-3 inline-flex items-center gap-2 text-sm font-semibold text-slate-500"><LoaderCircle className="size-4 animate-spin" /> Đang cập nhật...</p> : null}
    </div>
  );
}

const inputClassName = "rounded-xl border border-slate-200 px-3 py-2.5 text-sm font-medium outline-none focus:border-green-500 focus:ring-4 focus:ring-green-100";

function Select({
  value,
  onChange,
  options,
  placeholder,
}: {
  value: string;
  onChange: (value: string) => void;
  options: ReadonlyArray<readonly [string, string]>;
  placeholder?: string;
}) {
  return (
    <select value={value} onChange={(event) => onChange(event.target.value)} className={inputClassName}>
      {placeholder ? <option value="">{placeholder}</option> : null}
      {options.map(([optionValue, label]) => <option key={optionValue} value={optionValue}>{label}</option>)}
    </select>
  );
}

function Pagination({ current, total, filters, basePath }: { current: number; total: number; filters: CommunityPostFilters; basePath: string }) {
  if (total <= 1) return null;
  return (
    <div className="mt-8 flex justify-center gap-3">
      {current > 1 ? <Link className="rounded-xl border border-slate-200 px-4 py-2 text-sm font-bold text-slate-700" href={href(current - 1, filters, basePath)}><ChevronLeft className="inline size-4" /> Trước</Link> : null}
      {current < total ? <Link className="rounded-xl bg-green-600 px-4 py-2 text-sm font-bold text-white" href={href(current + 1, filters, basePath)}>Sau <ChevronRight className="inline size-4" /></Link> : null}
    </div>
  );
}

function href(page: number, filters: CommunityPostFilters, basePath: string) {
  const params = new URLSearchParams();
  Object.entries(filters).forEach(([key, value]) => {
    if (value && key !== "ownerId" && key !== "applicantId") params.set(key, String(value));
  });
  params.set("page", String(page));
  return `${basePath}?${params}`;
}

function navClass(active: boolean) {
  return `inline-flex items-center justify-center rounded-xl border px-4 py-2.5 text-sm font-black ${active
    ? "border-green-600 bg-green-600 text-white"
    : "border-slate-200 bg-white text-slate-700 hover:border-green-400"
    }`;
}

function State({ title }: { title: string }) {
  return <div className="rounded-2xl border border-dashed border-slate-300 bg-white p-10 text-center font-bold text-slate-600">{title}</div>;
}

function FieldTypeBadge({ value, size = "md" }: { value: string | null | undefined; size?: "sm" | "md" }) {
  const sizeClassName = size === "sm" ? "h-8 rounded-lg px-3 text-xs" : "h-10 rounded-lg px-4 text-sm";
  return (
    <span className={`inline-flex items-center justify-center border border-slate-200 bg-slate-50 font-black leading-none text-slate-700 ${sizeClassName}`}>
      {formatFieldType(value)}
    </span>
  );
}

function FeedSkeleton() {
  return <div className="grid gap-5 xl:grid-cols-2">{Array.from({ length: 4 }, (_, i) => <div key={i} className="h-64 animate-pulse rounded-2xl bg-slate-200" />)}</div>;
}

function formatPercent(value: number) {
  return `${Number(value ?? 0).toFixed(1).replace(/\.0$/, "")}%`;
}
