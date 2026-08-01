"use client";

import { useEffect, useState, type FormEvent } from "react";
import { Provinces } from "vietnam-divisions-js";
import type { District } from "vietnam-divisions-js/districts";
import type { Province } from "vietnam-divisions-js/provinces";
import { SlidersHorizontal, Search, X } from "lucide-react";

const sortOptions = [
  { label: "Đánh giá cao", sortBy: "rating", direction: "desc" },
  { label: "Nhiều đánh giá", sortBy: "reviews", direction: "desc" },
  { label: "Mới nhất", sortBy: "newest", direction: "desc" },
] as const;

const distanceOptions = [
  { label: "Tất cả", value: "" },
  { label: "≤ 2 km", value: "2" },
  { label: "≤ 5 km", value: "5" },
  { label: "≤ 10 km", value: "10" },
  { label: "≤ 20 km", value: "20" },
] as const;

export function LandingFieldSearch() {
  const [open, setOpen] = useState(false);
  const [provinces, setProvinces] = useState<Province[]>([]);
  const [districts, setDistricts] = useState<District[]>([]);
  const [provinceCode, setProvinceCode] = useState("");
  const [district, setDistrict] = useState("");
  const [radiusKm, setRadiusKm] = useState("");
  const [sort, setSort] = useState<(typeof sortOptions)[number]>(sortOptions[0]);
  const [latitude, setLatitude] = useState("");
  const [longitude, setLongitude] = useState("");
  const [locationError, setLocationError] = useState<string | null>(null);

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
    if (!provinceCode) return;
    let active = true;
    Provinces.getDistrictsByProvinceId(provinceCode).then((items) => {
      if (active) setDistricts(items);
    });
    return () => {
      active = false;
    };
  }, [provinceCode]);

  function selectRadius(value: string) {
    setRadiusKm(value);
    setLocationError(null);
    if (!value) {
      setLatitude("");
      setLongitude("");
      return;
    }
    if (!navigator.geolocation) {
      setLocationError("Trình duyệt không hỗ trợ định vị.");
      return;
    }
    navigator.geolocation.getCurrentPosition(
      ({ coords }) => {
        setLatitude(coords.latitude.toFixed(6));
        setLongitude(coords.longitude.toFixed(6));
      },
      () => {
        setRadiusKm("");
        setLocationError("Vui lòng cấp quyền định vị để lọc theo khoảng cách.");
      },
      { enableHighAccuracy: true, timeout: 10_000 },
    );
  }

  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    const params = new URLSearchParams();
    const keyword = String(form.get("keyword") ?? "").trim();
    if (keyword) params.set("keyword", keyword);
    if (provinceCode) params.set("provinceCode", provinceCode);
    if (district) params.set("district", district);
    if (radiusKm && latitude && longitude) {
      params.set("radiusKm", radiusKm);
      params.set("latitude", latitude);
      params.set("longitude", longitude);
      params.set("sortBy", "distance");
      params.set("direction", "asc");
    } else {
      params.set("sortBy", sort.sortBy);
      params.set("direction", sort.direction);
    }
    window.location.href = `/fields${params.size ? `?${params}` : ""}`;
  }

  function resetFilters() {
    setProvinceCode("");
    setDistrict("");
    setDistricts([]);
    setRadiusKm("");
    setLatitude("");
    setLongitude("");
    setSort(sortOptions[0]);
    setLocationError(null);
  }

  return (
    <form onSubmit={submit} className="space-y-3">
      <div className="grid gap-3 md:grid-cols-[1fr_auto]">
        <label className="flex min-h-14 items-center gap-3 rounded-2xl border border-slate-200 bg-white px-4 text-base font-semibold text-slate-700 shadow-sm focus-within:border-green-500 focus-within:ring-4 focus-within:ring-green-100">
          <Search className="size-5 shrink-0 text-slate-400" aria-hidden="true" />
          <span className="sr-only">Tìm sân</span>
          <input
            name="keyword"
            type="search"
            placeholder="Tìm theo tên sân hoặc khu vực..."
            className="min-w-0 flex-1 bg-transparent py-3 outline-none placeholder:text-slate-400"
          />
        </label>
        <button
          type="button"
          onClick={() => setOpen((value) => !value)}
          aria-expanded={open}
          className="inline-flex min-h-14 items-center justify-center gap-2 rounded-2xl bg-green-600 px-6 text-base font-black text-white hover:bg-green-700"
        >
          <SlidersHorizontal className="size-5" aria-hidden="true" />
          Bộ lọc
        </button>
      </div>

      {open ? (
        <div className="rounded-2xl border border-slate-200 bg-white p-6 shadow-[0_16px_40px_rgba(15,23,42,0.08)]">
          <div className="mb-6 flex items-center justify-between gap-4">
            <h3 className="text-base font-black text-slate-950">Bộ lọc tìm kiếm</h3>
            <button
              type="button"
              onClick={() => setOpen(false)}
              className="grid size-9 place-items-center rounded-full text-slate-400 hover:bg-slate-100 hover:text-slate-700"
              aria-label="Đóng bộ lọc"
            >
              <X className="size-5" aria-hidden="true" />
            </button>
          </div>

          <div className="grid gap-5 lg:grid-cols-[1fr_1fr_0.9fr_1fr]">
            <label className="text-lg font-semibold text-slate-700">
              Tỉnh / Thành phố
              <select
                value={provinceCode}
                onChange={(event) => {
                  setProvinceCode(event.target.value);
                  setDistrict("");
                  setDistricts([]);
                }}
                className="mt-2 min-h-11 w-full rounded-xl border border-slate-200 bg-slate-50 px-4 text-base outline-none focus:border-green-500 focus:bg-white"
              >
                <option value="">Tất cả</option>
                {provinces.map((province) => (
                  <option key={province.idProvince} value={province.idProvince}>
                    {province.name}
                  </option>
                ))}
              </select>
            </label>

            <label className="text-lg font-semibold text-slate-700">
              Quận / Huyện
              <select
                value={district}
                disabled={!provinceCode}
                onChange={(event) => setDistrict(event.target.value)}
                className="mt-2 min-h-11 w-full rounded-xl border border-slate-200 bg-slate-50 px-4 text-base outline-none focus:border-green-500 focus:bg-white disabled:text-slate-400"
              >
                <option value="">Tất cả</option>
                {districts.map((item) => (
                  <option key={item.idDistrict} value={item.name}>
                    {item.name}
                  </option>
                ))}
              </select>
            </label>

            <fieldset>
              <legend className="text-lg font-semibold text-slate-700">Khoảng cách</legend>
              <div className="mt-2 flex flex-wrap gap-2">
                {distanceOptions.map((option) => (
                  <button
                    key={option.value}
                    type="button"
                    onClick={() => selectRadius(option.value)}
                    className={`min-h-10 rounded-xl border px-4 text-base font-bold ${
                      radiusKm === option.value
                        ? "border-green-600 bg-green-600 text-white"
                        : "border-slate-200 bg-white text-slate-700 hover:border-green-300"
                    }`}
                  >
                    {option.label}
                  </button>
                ))}
              </div>
              {locationError ? (
                <p className="mt-2 text-sm font-semibold text-rose-600">{locationError}</p>
              ) : null}
            </fieldset>

            <fieldset>
              <legend className="text-lg font-semibold text-slate-700">Sắp xếp theo</legend>
              <div className="mt-2 flex flex-wrap gap-2">
                {sortOptions.map((option) => (
                  <button
                    key={option.label}
                    type="button"
                    onClick={() => setSort(option)}
                    className={`min-h-10 rounded-xl border px-4 text-base font-bold ${
                      sort.label === option.label
                        ? "border-green-600 bg-green-600 text-white"
                        : "border-slate-200 bg-white text-slate-700 hover:border-green-300"
                    }`}
                  >
                    {option.label}
                  </button>
                ))}
              </div>
            </fieldset>
          </div>

          <div className="mt-8 flex justify-end gap-3 border-t border-slate-200 pt-5">
            <button
              type="button"
              onClick={resetFilters}
              className="inline-flex min-h-11 items-center justify-center rounded-xl border border-slate-200 bg-white px-5 text-base font-bold text-slate-600 hover:bg-slate-50"
            >
              Đặt lại
            </button>
            <button className="inline-flex min-h-11 items-center justify-center rounded-xl bg-green-600 px-6 text-base font-black text-white hover:bg-green-700">
              Áp dụng
            </button>
          </div>
        </div>
      ) : null}
    </form>
  );
}
