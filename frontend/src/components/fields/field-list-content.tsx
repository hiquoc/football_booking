"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useState, useTransition, type FormEvent } from "react";
import { Provinces } from "vietnam-divisions-js";
import type { District } from "vietnam-divisions-js/districts";
import type { Province } from "vietnam-divisions-js/provinces";
import { ChevronLeft, ChevronRight, CircleAlert, LoaderCircle, LocateFixed, Search, X } from "lucide-react";
import { useFieldCards } from "@/lib/hooks/use-fields";
import type { FieldCardFilters, User } from "@/lib/api/types";
import { fieldTypeOptions, subFieldTypeOptions } from "@/lib/field-format";
import { FieldCard } from "./field-card";

export function FieldListContent({
  pageNumber,
  filters,
  viewerRole,
}: {
  pageNumber: number;
  filters: FieldCardFilters;
  viewerRole: User["userType"] | null;
}) {
  const { data, isPending, isError } = useFieldCards(pageNumber - 1, 9, filters);
  const [isFiltering, setIsFiltering] = useState(false);
  const isLoading = isPending || isFiltering;

  return (
    <>
      <FieldFilters
        key={JSON.stringify(filters)}
        filters={filters}
        onPendingChange={setIsFiltering}
      />
      {isLoading ? <FieldListSkeleton /> : null}
      {!isLoading && isError ? <FieldListError /> : null}
      {!isLoading && data && !data.content.length ? <EmptyFieldList /> : null}
      {!isLoading && data?.content.length ? (
        <>
          <div className="mb-7 flex items-end justify-between gap-4">
            <div>
              <p className="text-sm font-semibold text-slate-500">
                Tìm thấy {data.totalElements} địa điểm
              </p>
              <h2 className="mt-1 text-2xl font-black text-slate-950">
                Sân phù hợp
              </h2>
            </div>
            <span className="text-sm font-semibold text-slate-400">
              Trang {data.page + 1}/{Math.max(data.totalPages, 1)}
            </span>
          </div>
          <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-3">
            {data.content.map((field) => (
              <FieldCard
                key={field.id}
                field={field}
                canFavorite={viewerRole === "CLIENT" || viewerRole === "EMPLOYEE"}
              />
            ))}
          </div>
          <Pagination current={data.page + 1} total={data.totalPages} filters={filters} />
        </>
      ) : null}
    </>
  );
}

function FieldFilters({
  filters,
  onPendingChange,
}: {
  filters: FieldCardFilters;
  onPendingChange: (pending: boolean) => void;
}) {
  const router = useRouter();
  const [isPending, startTransition] = useTransition();
  const [isLocating, setIsLocating] = useState(false);
  const [locationError, setLocationError] = useState<string | null>(null);
  const [provinces, setProvinces] = useState<Province[]>([]);
  const [districts, setDistricts] = useState<District[]>([]);
  const [selectedProvinceCode, setSelectedProvinceCode] = useState(filters.provinceCode ?? "");
  const [selectedDistrict, setSelectedDistrict] = useState(filters.district ?? "");
  const [keywordInput, setKeywordInput] = useState(filters.keyword ?? "");
  const sortValue = `${filters.sortBy ?? "rating"}:${filters.direction ?? "desc"}`;
  const isNearby = Boolean(
    filters.latitude &&
      filters.longitude &&
      filters.sortBy === "distance" &&
      filters.direction === "asc",
  );

  useEffect(() => {
    onPendingChange(isPending);
  }, [isPending, onPendingChange]);

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
    if (!selectedProvinceCode) {
      return;
    }
    let active = true;
    Provinces.getDistrictsByProvinceId(selectedProvinceCode).then((items) => {
      if (active) setDistricts(items);
    });
    return () => {
      active = false;
    };
  }, [selectedProvinceCode]);

  const navigate = (params: URLSearchParams) => {
    params.delete("page");
    startTransition(() => router.push(`/fields${params.size ? `?${params}` : ""}`));
  };

  useEffect(() => {
    const timeout = window.setTimeout(() => {
      const keyword = keywordInput.trim();
      if (keyword === (filters.keyword ?? "")) return;
      const params = filtersToParams(filters);
      params.delete("page");
      if (keyword) {
        params.set("keyword", keyword);
      } else {
        params.delete("keyword");
      }
      startTransition(() => router.replace(`/fields${params.size ? `?${params}` : ""}`));
    }, 500);

    return () => window.clearTimeout(timeout);
  }, [filters, keywordInput, router]);

  const submit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    const params = new URLSearchParams();
    ["keyword", "fieldType", "subFieldType", "district", "provinceCode", "latitude", "longitude", "radiusKm"].forEach((key) => {
      const value = String(form.get(key) ?? "").trim();
      if (value) params.set(key, value);
    });
    const [sortBy, direction] = String(form.get("sort") ?? "rating:desc").split(":");
    params.set("sortBy", sortBy);
    params.set("direction", direction);
    navigate(params);
  };

  const useMyLocation = () => {
    setLocationError(null);
    if (isNearby) {
      const params = filtersToParams(filters);
      params.delete("latitude");
      params.delete("longitude");
      params.delete("radiusKm");
      params.set("sortBy", "rating");
      params.set("direction", "desc");
      navigate(params);
      return;
    }
    if (!navigator.geolocation) {
      setLocationError("Trình duyệt không hỗ trợ định vị.");
      return;
    }
    setIsLocating(true);
    navigator.geolocation.getCurrentPosition(
      ({ coords }) => {
        setIsLocating(false);
        const params = filtersToParams(filters);
        params.set("latitude", coords.latitude.toFixed(6));
        params.set("longitude", coords.longitude.toFixed(6));
        params.set("radiusKm", filters.radiusKm ?? "10");
        params.set("sortBy", "distance");
        params.set("direction", "asc");
        navigate(params);
      },
      (error) => {
        setIsLocating(false);
        setLocationError(
          error.code === error.PERMISSION_DENIED
            ? "Vui lòng cấp quyền định vị để tìm sân gần bạn."
            : "Không thể lấy vị trí hiện tại. Vui lòng thử lại.",
        );
      },
      { enableHighAccuracy: true, timeout: 10_000 },
    );
  };

  return (
    <form onSubmit={submit} className="mb-10 rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
      <label className="mb-4 block text-sm font-bold text-slate-700">
        Tên sân
        <input
          name="keyword"
          type="search"
          value={keywordInput}
          onChange={(event) => setKeywordInput(event.target.value)}
          placeholder="Nhập tên sân"
          className="mt-1.5 w-full rounded-xl border border-slate-200 bg-white px-3 py-2.5 font-medium outline-none focus:border-green-500 focus:ring-4 focus:ring-green-100"
        />
      </label>
      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
        <FilterSelect name="fieldType" label="Môn thể thao" defaultValue={filters.fieldType} options={fieldTypeOptions} />
        <FilterSelect name="subFieldType" label="Loại sân" defaultValue={filters.subFieldType} options={subFieldTypeOptions} />
        <label className="text-sm font-bold text-slate-700">
          Tỉnh / thành phố
          <select
            name="provinceCode"
            value={selectedProvinceCode}
            onChange={(event) => {
              setSelectedProvinceCode(event.target.value);
              setSelectedDistrict("");
              setDistricts([]);
            }}
            className="mt-1.5 w-full rounded-xl border border-slate-200 bg-white px-3 py-2.5 font-medium outline-none focus:border-green-500 focus:ring-4 focus:ring-green-100"
          >
            <option value="">Tất cả</option>
            {provinces.map((province) => (
              <option key={province.idProvince} value={province.idProvince}>
                {province.name}
              </option>
            ))}
          </select>
        </label>
        <label className="text-sm font-bold text-slate-700">
          Quận / huyện
          <select
            name="district"
            value={selectedDistrict}
            disabled={!selectedProvinceCode}
            onChange={(event) => setSelectedDistrict(event.target.value)}
            className="mt-1.5 w-full rounded-xl border border-slate-200 bg-white px-3 py-2.5 font-medium outline-none focus:border-green-500 focus:ring-4 focus:ring-green-100 disabled:bg-slate-100 disabled:text-slate-400"
          >
            <option value="">Tất cả</option>
            {districts.map((district) => (
              <option key={district.idDistrict} value={district.name}>
                {district.name}
              </option>
            ))}
          </select>
        </label>
      </div>
      <div className="mt-4 flex flex-col gap-3 border-t border-slate-100 pt-4 sm:flex-row sm:items-end">
        <label className="flex-1 text-sm font-bold text-slate-700">
          Sắp xếp
          <select name="sort" defaultValue={sortValue} className="mt-1.5 w-full rounded-xl border border-slate-200 bg-white px-3 py-2.5 font-medium outline-none focus:border-green-500 focus:ring-4 focus:ring-green-100">
            <option value="rating:desc">Đánh giá cao nhất</option>
            <option value="reviews:desc">Nhiều đánh giá nhất</option>
            <option value="newest:desc">Mới nhất</option>
            {filters.latitude && filters.longitude ? <option value="distance:asc">Gần tôi nhất</option> : null}
          </select>
        </label>
        <input type="hidden" name="latitude" value={filters.latitude ?? ""} />
        <input type="hidden" name="longitude" value={filters.longitude ?? ""} />
        <input type="hidden" name="radiusKm" value={filters.radiusKm ?? ""} />
        <button
          type="button"
          onClick={useMyLocation}
          disabled={isLocating || isPending}
          aria-pressed={isNearby}
          className={`inline-flex items-center justify-center gap-2 rounded-xl border px-4 py-2.5 text-sm font-bold transition disabled:cursor-wait disabled:opacity-70 ${
            isNearby
              ? "border-green-600 bg-green-600 text-white shadow-sm shadow-green-200"
              : "border-green-200 text-green-700 hover:bg-green-50"
          }`}
        >
          {isLocating ? (
            <LoaderCircle className="size-4 animate-spin" />
          ) : (
            <LocateFixed className="size-4" />
          )}
          {isLocating ? "Đang định vị..." : isNearby ? "Tắt lọc gần tôi" : "Gần tôi"}
        </button>
        <button disabled={isLocating || isPending} className="inline-flex items-center justify-center gap-2 rounded-xl bg-green-600 px-5 py-2.5 text-sm font-bold text-white hover:bg-green-700 disabled:cursor-wait disabled:opacity-60">
          <Search className="size-4" /> Lọc sân
        </button>
        <Link href="/fields" className="inline-flex items-center justify-center gap-2 rounded-xl px-4 py-2.5 text-sm font-bold text-slate-500 hover:bg-slate-100">
          <X className="size-4" /> Xóa lọc
        </Link>
      </div>
      {locationError ? <p className="mt-3 text-sm font-medium text-rose-600">{locationError}</p> : null}
    </form>
  );
}

function FilterSelect({ name, label, defaultValue, options }: { name: string; label: string; defaultValue?: string; options: ReadonlyArray<readonly [string, string]> }) {
  return (
    <label className="text-sm font-bold text-slate-700">
      {label}
      <select name={name} defaultValue={defaultValue ?? ""} className="mt-1.5 w-full rounded-xl border border-slate-200 bg-white px-3 py-2.5 font-medium outline-none focus:border-green-500 focus:ring-4 focus:ring-green-100">
        <option value="">Tất cả</option>
        {options.map(([value, text]) => <option key={value} value={value}>{text}</option>)}
      </select>
    </label>
  );
}

function FieldListError() {
  return (
    <div className="rounded-[2rem] border border-amber-200 bg-amber-50 p-8 text-amber-950">
      <CircleAlert className="size-7" />
      <h2 className="mt-4 text-xl font-black">
        Tạm thời không thể tải danh sách sân
      </h2>
      <p className="mt-2 text-sm text-amber-900/75">
        Không thể kết nối đến dịch vụ sân. Vui lòng thử lại sau.
      </p>
    </div>
  );
}

function EmptyFieldList() {
  return (
    <div className="rounded-[2rem] border border-dashed border-slate-300 bg-white p-12 text-center">
      <h2 className="text-2xl font-black text-slate-900">
        Chưa tìm thấy sân nào
      </h2>
      <p className="mt-2 text-slate-500">
        Các sân đã được duyệt sẽ xuất hiện tại đây.
      </p>
      <Link
        href="/fields"
        className="mt-6 inline-flex rounded-full bg-green-600 px-5 py-3 text-sm font-black text-white"
      >
        Xóa bộ lọc
      </Link>
    </div>
  );
}

function Pagination({
  current,
  total,
  filters,
}: {
  current: number;
  total: number;
  filters: FieldCardFilters;
}) {
  if (total <= 1) return null;
  return (
    <nav
      aria-label="Phân trang danh sách sân"
      className="mt-10 flex items-center justify-center gap-3"
    >
      {current > 1 ? (
        <Link
          href={fieldPageHref(current - 1, filters)}
          className="inline-flex items-center gap-2 rounded-full border border-slate-200 bg-white px-4 py-2.5 text-sm font-bold text-slate-700 hover:border-green-400"
        >
          <ChevronLeft className="size-4" /> Trang trước
        </Link>
      ) : null}
      {current < total ? (
        <Link
          href={fieldPageHref(current + 1, filters)}
          className="inline-flex items-center gap-2 rounded-full bg-slate-950 px-4 py-2.5 text-sm font-bold text-white hover:bg-green-600 hover:text-white"
        >
          Trang sau <ChevronRight className="size-4" />
        </Link>
      ) : null}
    </nav>
  );
}

function filtersToParams(filters: FieldCardFilters) {
  const params = new URLSearchParams();
  Object.entries(filters).forEach(([key, value]) => {
    if (value) params.set(key, value);
  });
  return params;
}

function fieldPageHref(page: number, filters: FieldCardFilters) {
  const params = filtersToParams(filters);
  params.set("page", String(page));
  return `/fields?${params}`;
}

function FieldListSkeleton() {
  return (
    <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-3">
      {[0, 1, 2, 3, 4, 5].map((item) => (
        <div
          key={item}
          className="h-96 animate-pulse rounded-[1.75rem] bg-slate-200"
        />
      ))}
    </div>
  );
}
