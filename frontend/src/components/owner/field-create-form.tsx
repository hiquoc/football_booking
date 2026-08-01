"use client";

import { useState } from "react";
import { LoaderCircle, MapPin, Save } from "lucide-react";
import { useCreateField } from "@/lib/hooks/use-owner-fields";
import { LocationMapPicker } from "./location-map-picker";
import {
  emptyFieldLocation,
  type FieldLocationValue,
} from "./field-location-types";
import { VietnamLocationFields } from "./vietnam-location-fields";
import {
  closingTimeOptions,
  requireLocalTimePayload,
  toClosingTimeInputValue,
  toClosingTimePayload,
  toTimeInputValue,
} from "@/lib/time-format";

const days = [
  ["MONDAY", "Thứ Hai"],
  ["TUESDAY", "Thứ Ba"],
  ["WEDNESDAY", "Thứ Tư"],
  ["THURSDAY", "Thứ Năm"],
  ["FRIDAY", "Thứ Sáu"],
  ["SATURDAY", "Thứ Bảy"],
  ["SUNDAY", "Chủ Nhật"],
] as const;
const closeTimeOptions = closingTimeOptions();

export function FieldCreateForm() {
  const mutation = useCreateField();
  const [location, setLocation] = useState<FieldLocationValue>(
    emptyFieldLocation,
  );
  const [locationError, setLocationError] = useState<string | null>(null);
  const [hours, setHours] = useState(() =>
    days.map(([dayOfWeek]) => ({
      dayOfWeek,
      openTime: "06:00:00",
      closeTime: "23:00:00",
      closed: false,
      open24Hours: false,
    })),
  );

  async function submit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (location.latitude === null || location.longitude === null) {
      setLocationError("Vui lòng chọn vị trí sân trên bản đồ.");
      return;
    }
    setLocationError(null);
    const data = new FormData(event.currentTarget);
    try {
      const field = await mutation.mutateAsync({
        name: String(data.get("name")),
        description: String(data.get("description") || ""),
        address: location.address,
        ward: location.ward,
        wardCode: location.wardCode,
        province: location.province,
        provinceCode: location.provinceCode,
        legacyWard: location.legacyWard,
        legacyWardCode: location.legacyWardCode,
        legacyDistrict: location.legacyDistrict,
        legacyProvince: location.legacyProvince,
        latitude: location.latitude,
        longitude: location.longitude,
        phoneNumber: String(data.get("phoneNumber")),
        email: String(data.get("email") || ""),
        active: true,
        operatingHours: hours.map((item) =>
          item.closed || item.open24Hours
            ? {
                dayOfWeek: item.dayOfWeek,
                closed: item.closed,
                open24Hours: item.open24Hours,
              }
            : {
                ...item,
                openTime: requireLocalTimePayload(item.openTime),
                closeTime: toClosingTimePayload(item.closeTime, "23:00:00"),
              },
        ),
      });
      window.location.assign(`/fields/${field.id}`);
    } catch {
      /* Error is rendered below. */
    }
  }

  function updateHour(index: number, patch: Partial<(typeof hours)[number]>) {
    setHours((current) =>
      current.map((item, itemIndex) =>
        itemIndex === index ? { ...item, ...patch } : item,
      ),
    );
  }

  return (
    <form onSubmit={submit} className="space-y-7">
      <section className="rounded-[2rem] border border-slate-200 bg-white p-6 shadow-sm sm:p-8">
        <div className="flex items-center gap-3">
          <span className="grid size-11 place-items-center rounded-xl bg-green-100 text-green-700">
            <MapPin className="size-5" />
          </span>
          <div>
            <h2 className="text-xl font-black">Thông tin địa điểm</h2>
            <p className="text-sm text-slate-500">
              Cung cấp thông tin chính xác để người chơi dễ tìm sân.
            </p>
          </div>
        </div>
        <div className="mt-7 grid gap-5 sm:grid-cols-2">
          <Field label="Tên sân">
            <input
              required
              name="name"
              className="input-field"
              maxLength={100}
            />
          </Field>
          <Field label="Số điện thoại">
            <input
              required
              name="phoneNumber"
              className="input-field"
              type="tel"
            />
          </Field>
          <div className="sm:col-span-2">
            <VietnamLocationFields value={location} onChange={setLocation} />
          </div>
          <div className="sm:col-span-2">
            <LocationMapPicker
              latitude={location.latitude}
              longitude={location.longitude}
              onChange={(latitude, longitude) => {
                setLocation((current) => ({
                  ...current,
                  latitude,
                  longitude,
                }));
                setLocationError(null);
              }}
            />
            {locationError ? (
              <p className="mt-3 text-sm font-semibold text-rose-600">
                {locationError}
              </p>
            ) : null}
          </div>
          <Field label="Email">
            <input name="email" className="input-field" type="email" />
          </Field>
          <div className="sm:col-span-2">
            <Field label="Mô tả">
              <textarea
                name="description"
                rows={5}
                maxLength={2000}
                className="input-field resize-none"
              />
            </Field>
          </div>
        </div>
      </section>
      <section className="rounded-[2rem] border border-slate-200 bg-white p-6 shadow-sm sm:p-8">
        <h2 className="text-xl font-black">Giờ hoạt động</h2>
        <p className="mt-1 text-sm text-slate-500">
          Thiết lập đầy đủ lịch hoạt động trong tuần.
        </p>
        <div className="mt-6 space-y-3">
          {hours.map((item, index) => (
            <div
              key={item.dayOfWeek}
              className="grid items-center gap-3 rounded-2xl bg-slate-50 p-4 sm:grid-cols-[8rem_1fr_1fr_auto_auto]"
            >
              <strong className="text-sm">{days[index][1]}</strong>
              <input
                type="time"
                disabled={item.closed || item.open24Hours}
                value={toTimeInputValue(item.openTime, "06:00")}
                onChange={(event) => {
                  if (!event.target.value) return;
                  updateHour(index, { openTime: requireLocalTimePayload(event.target.value) });
                }}
                className="input-field disabled:opacity-40"
              />
              <input
                type="time"
                list="field-create-close-time-options"
                disabled={item.closed || item.open24Hours}
                value={toClosingTimeInputValue(item.closeTime, "23:00")}
                onChange={(event) => {
                  if (!event.target.value) return;
                  updateHour(index, { closeTime: toClosingTimePayload(event.target.value, "23:00:00") });
                }}
                className="input-field disabled:opacity-40"
              />
              <label className="inline-flex items-center gap-2 text-sm font-bold text-slate-600">
                <input
                  type="checkbox"
                  checked={item.open24Hours}
                  disabled={item.closed}
                  onChange={(event) =>
                    updateHour(index, { open24Hours: event.target.checked })
                  }
                />{" "}
                24h
              </label>
              <label className="inline-flex items-center gap-2 text-sm font-bold text-slate-600">
                <input
                  type="checkbox"
                  checked={item.closed}
                  onChange={(event) =>
                    updateHour(index, { closed: event.target.checked, open24Hours: false })
                  }
                />{" "}
                Đóng cửa
              </label>
            </div>
          ))}
        </div>
        <datalist id="field-create-close-time-options">
          {closeTimeOptions.map((option) => (
            <option key={option.value} value={option.value} label={option.label} />
          ))}
        </datalist>
      </section>
      {mutation.error ? (
        <p className="rounded-xl bg-rose-50 p-4 text-sm text-rose-700">
          {mutation.error.message}
        </p>
      ) : null}
      <button
        disabled={mutation.isPending}
        className="inline-flex items-center gap-2 rounded-full bg-green-600 px-6 py-3.5 text-sm font-black text-white disabled:opacity-60"
      >
        {mutation.isPending ? (
          <LoaderCircle className="size-4 animate-spin" />
        ) : (
          <Save className="size-4" />
        )}{" "}
        Tạo sân
      </button>
    </form>
  );
}
function Field({
  label,
  children,
}: {
  label: string;
  children: React.ReactNode;
}) {
  return (
    <label className="block">
      <span className="mb-2 block text-sm font-bold text-slate-700">
        {label}
      </span>
      {children}
    </label>
  );
}
