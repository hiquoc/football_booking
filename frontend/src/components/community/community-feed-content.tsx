"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useMemo, useState, useTransition } from "react";
import { Provinces } from "vietnam-divisions-js";
import type { District } from "vietnam-divisions-js/districts";
import type { Province } from "vietnam-divisions-js/provinces";
import { CalendarDays, ChevronLeft, ChevronRight, LoaderCircle, Trophy, Users } from "lucide-react";
import type { CommunityPostFilters } from "@/lib/api/types";
import { useFieldCards } from "@/lib/hooks/use-fields";
import { useCommunityPosts } from "@/lib/hooks/use-community";
import { postStatusLabels, postTypeLabels, skillLabel, skillLevelOptions, timeRange } from "./community-labels";

const postTypes = [
  ["LOOKING_OPPONENT", "Tim doi thu"],
  ["LOOKING_PLAYER", "Tim cau thu"],
] as const;

const fieldTypes = [
  ["FOOTBALL_5V5", "Bong da 5 nguoi"],
  ["FOOTBALL_7V7", "Bong da 7 nguoi"],
  ["FOOTBALL_11V11", "Bong da 11 nguoi"],
  ["BASKETBALL_HALF_COURT", "Bong ro nua san"],
  ["BASKETBALL_FULL_COURT", "Bong ro toan san"],
  ["BADMINTON", "Cau long"],
  ["VOLLEYBALL", "Bong chuyen"],
  ["TENNIS", "Quan vot"],
] as const;

const statuses = [
  ["all", "Tat ca"],
  ["OPEN", "Dang mo"],
  ["MATCHED", "Da ghep doi"],
  ["FULL", "Da du nguoi"],
  ["CLOSED", "Da dong"],
  ["CANCELLED", "Da Huy"],
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
  const sortedFilters = { ...filters, sortBy: filters.sortBy ?? "upcoming" } satisfies CommunityPostFilters;
  const { data, isPending, isError } = useCommunityPosts(pageNumber - 1, 10, sortedFilters);

  return (
    <div className="mx-auto w-full max-w-[90rem] px-5 py-10 sm:px-8">
      <div className="mb-8 flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <p className="text-sm font-bold uppercase text-emerald-600">Cong dong</p>
          <h1 className="mt-2 text-4xl font-black text-slate-950">Tim doi va cau thu</h1>
          <p className="mt-3 max-w-2xl text-slate-600">
            Cac bai dang gan voi lich dat san da xac nhan, sap xep theo ngay va gio thi dau.
          </p>
        </div>
        {canCreate ? (
          <div className="flex w-full flex-col gap-2 sm:w-auto">
            {viewerId ? (
              <>
                <Link href={`/community?date=${encodeURIComponent(tomorrowDate())}&sortBy=upcoming`} className={navClass(basePath === "/community", "emerald")}>
                  All Post
                </Link>
                <Link href="/community/my-posts?status=all&sortBy=newest" className={navClass(basePath === "/community/my-posts", "emerald")}>
                  My post
                </Link>
                <Link href="/community/my-applications?status=all&sortBy=newest" className={navClass(basePath === "/community/my-applications", "sky")}>
                  My applicant
                </Link>
              </>
            ) : null}
            <Link href="/community/new" className="inline-flex items-center justify-center rounded-xl bg-emerald-600 px-5 py-3 text-sm font-black text-white hover:bg-emerald-700">
              Dang bai moi
            </Link>
          </div>
        ) : null}
      </div>
      <CommunityFilters filters={sortedFilters} basePath={basePath} />
      {isPending ? <FeedSkeleton /> : null}
      {!isPending && isError ? <State title="Khong the tai bai dang" /> : null}
      {!isPending && data?.empty ? <State title="Chua co bai dang phu hop" /> : null}
      {!isPending && data?.content.length ? (
        <>
          <div className="mb-4 flex justify-between text-sm font-semibold text-slate-500">
            <span>{data.totalElements} bai dang</span>
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
                    <span className="inline-flex items-center gap-2"><Users className="size-4" /> Can {post.playersNeeded} · Da nhan {post.acceptedPlayersCount}</span>
                  ) : (
                    <span className="inline-flex items-center gap-2"><Trophy className="size-4" /> Dang tim doi thu</span>
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
                    Xem chi tiet
                  </Link>
                  <Link href={`/users/${post.ownerId}/profile`} className="rounded-lg border border-slate-200 px-4 py-2 text-sm font-bold text-slate-700 hover:border-emerald-400">
                    Xem ho so
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
    sortBy: filters.sortBy ?? "upcoming",
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
        if (value.trim()) params.set(key, value.trim());
      });
      startTransition(() => router.push(`${basePath}?${params}`));
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
    <div className="mb-8 rounded-lg border border-slate-200 bg-white p-4 shadow-sm">
      <div className="grid gap-3 md:grid-cols-4">
        <input value={values.keyword} onChange={(event) => update("keyword", event.target.value)} placeholder="Tu khoa" className="rounded-lg border border-slate-200 px-3 py-2 text-sm font-medium outline-none focus:border-emerald-500" />
        <Select value={values.postType} onChange={(value) => update("postType", value)} options={postTypes} placeholder="Tat ca loai bai" />
        <input value={values.date} onChange={(event) => update("date", event.target.value)} type="date" className="rounded-lg border border-slate-200 px-3 py-2 text-sm font-medium" />
        <Select value={values.skillLevel} onChange={(value) => update("skillLevel", value)} options={skillLevelOptions} placeholder="Tat ca trinh do" />
        <Select value={values.fieldType} onChange={(value) => update("fieldType", value)} options={fieldTypes} placeholder="Tat ca loai san" />
        <select value={values.city} onChange={(event) => update("city", event.target.value)} className="rounded-lg border border-slate-200 px-3 py-2 text-sm font-medium">
          <option value="">Tat ca tinh/thanh</option>
          {provinces.map((province) => (
            <option key={province.idProvince} value={province.name}>{province.name}</option>
          ))}
        </select>
        <select value={values.district} onChange={(event) => update("district", event.target.value)} disabled={!values.city} className="rounded-lg border border-slate-200 px-3 py-2 text-sm font-medium disabled:bg-slate-100 disabled:text-slate-400">
          <option value="">Tat ca quan/huyen</option>
          {districts.map((district) => (
            <option key={district.idDistrict} value={district.name}>{district.name}</option>
          ))}
        </select>
        <div className="relative">
          <input value={values.fieldName} onChange={(event) => update("fieldName", event.target.value)} placeholder="Ten san" className="w-full rounded-lg border border-slate-200 px-3 py-2 text-sm font-medium outline-none focus:border-emerald-500" />
          {showFieldSuggestions ? (
            <div className="absolute z-20 mt-1 max-h-60 w-full overflow-auto rounded-lg border border-slate-200 bg-white py-1 text-sm shadow-lg">
              {fieldSuggestions.data?.content.map((field) => (
                <button
                  key={field.id}
                  type="button"
                  onClick={() => update("fieldName", field.name)}
                  className="block w-full px-3 py-2 text-left font-medium text-slate-700 hover:bg-emerald-50"
                >
                  {field.name}
                </button>
              ))}
            </div>
          ) : null}
        </div>
        <Select value={values.status} onChange={(value) => update("status", value)} options={statuses} />
      </div>
      {isPending ? <p className="mt-3 inline-flex items-center gap-2 text-sm font-semibold text-slate-500"><LoaderCircle className="size-4 animate-spin" /> Dang cap nhat...</p> : null}
    </div>
  );
}

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
    <select value={value} onChange={(event) => onChange(event.target.value)} className="rounded-lg border border-slate-200 px-3 py-2 text-sm font-medium">
      {placeholder ? <option value="">{placeholder}</option> : null}
      {options.map(([optionValue, label]) => <option key={optionValue} value={optionValue}>{label}</option>)}
    </select>
  );
}

function Pagination({ current, total, filters, basePath }: { current: number; total: number; filters: CommunityPostFilters; basePath: string }) {
  if (total <= 1) return null;
  return (
    <div className="mt-8 flex justify-center gap-3">
      {current > 1 ? <Link className="rounded-lg border px-4 py-2 text-sm font-bold" href={href(current - 1, filters, basePath)}><ChevronLeft className="inline size-4" /> Truoc</Link> : null}
      {current < total ? <Link className="rounded-lg bg-slate-950 px-4 py-2 text-sm font-bold text-white" href={href(current + 1, filters, basePath)}>Sau <ChevronRight className="inline size-4" /></Link> : null}
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

function navClass(active: boolean, color: "emerald" | "sky") {
  const activeClass = color === "sky"
    ? "border-sky-600 bg-sky-50 text-sky-800"
    : "border-emerald-600 bg-emerald-50 text-emerald-800";
  const inactiveClass = color === "sky"
    ? "border-sky-200 text-sky-700 hover:bg-sky-50"
    : "border-emerald-200 text-emerald-700 hover:bg-emerald-50";
  return `inline-flex items-center justify-center rounded-xl border px-5 py-3 text-sm font-black ${active ? activeClass : inactiveClass}`;
}

function tomorrowDate() {
  const date = new Date();
  date.setDate(date.getDate() + 1);
  return [
    date.getFullYear(),
    String(date.getMonth() + 1).padStart(2, "0"),
    String(date.getDate()).padStart(2, "0"),
  ].join("-");
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
