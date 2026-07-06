"use client";

import { useState } from "react";
import { LoaderCircle, Save } from "lucide-react";
import type { Field, OperatingHours } from "@/lib/api/types";
import { useFieldEditorData } from "@/lib/hooks/use-fields";
import { useUpdateField } from "@/lib/hooks/use-owner-fields";
import { DataError, ListSkeleton } from "@/components/ui/data-state";
import { formatDay } from "@/lib/field-format";
import { LocationMapPicker } from "./location-map-picker";
import type { FieldLocationValue } from "./field-location-types";
import { VietnamLocationFields } from "./vietnam-location-fields";

export function FieldEditor({ fieldId }: { fieldId: string }) {
  const data = useFieldEditorData(fieldId);
  if (data.field.isPending || data.operatingHours.isPending)
    return <ListSkeleton count={3} />;
  if (data.field.isError || data.operatingHours.isError)
    return <DataError title="Không thể tải thông tin sân" />;
  return (
    <EditorForm
      key={data.field.data.updatedAt}
      field={data.field.data}
      hours={data.operatingHours.data}
    />
  );
}

function EditorForm({
  field,
  hours,
}: {
  field: Field;
  hours: OperatingHours[];
}) {
  const update = useUpdateField(field.id);
  const [location, setLocation] = useState<FieldLocationValue>({
    address: field.address,
    ward: field.ward,
    wardCode: field.wardCode,
    province: field.province,
    provinceCode: field.provinceCode,
    legacyWard: field.legacyWard,
    legacyWardCode: field.legacyWardCode,
    legacyDistrict: field.legacyDistrict,
    legacyProvince: field.legacyProvince,
    latitude: field.latitude,
    longitude: field.longitude,
  });
  const [locationError, setLocationError] = useState<string | null>(null);
  const [schedule, setSchedule] = useState(
    hours.map((item) => ({
      dayOfWeek: item.dayOfWeek,
      openTime: item.openTime ?? "06:00:00",
      closeTime: item.closeTime ?? "23:00:00",
      closed: item.closed,
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
      await update.mutateAsync({
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
        active: data.get("active") === "on",
        operatingHours: schedule,
      });
    } catch {
      /* Rendered below. */
    }
  }
  return (
    <form onSubmit={submit} className="space-y-6">
      <section className="rounded-[1.5rem] border border-slate-200 bg-white p-6">
        <div className="grid gap-5 sm:grid-cols-2">
          <FieldLabel label="Tên sân">
            <input
              name="name"
              required
              defaultValue={field.name}
              className="input-field"
            />
          </FieldLabel>
          <FieldLabel label="Số điện thoại">
            <input
              name="phoneNumber"
              required
              defaultValue={field.phoneNumber}
              className="input-field"
            />
          </FieldLabel>
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
          <FieldLabel label="Email">
            <input
              name="email"
              type="email"
              defaultValue={field.email ?? ""}
              className="input-field"
            />
          </FieldLabel>
          <label className="flex items-center gap-2 self-end pb-4 text-sm font-bold">
            <input
              name="active"
              type="checkbox"
              defaultChecked={field.active}
            />{" "}
            Đang hoạt động
          </label>
          <div className="sm:col-span-2">
            <FieldLabel label="Mô tả">
              <textarea
                name="description"
                rows={4}
                defaultValue={field.description ?? ""}
                className="input-field resize-none"
              />
            </FieldLabel>
          </div>
        </div>
      </section>
      <section className="rounded-[1.5rem] border border-slate-200 bg-white p-6">
        <h2 className="text-xl font-black">Giờ hoạt động</h2>
        <div className="mt-5 space-y-3">
          {schedule.map((item, index) => (
            <div
              key={item.dayOfWeek}
              className="items-center gap-3 rounded-xl bg-slate-50 p-3 grid sm:grid-cols-[8rem_1fr_1fr_auto]"
            >
              <strong className="text-sm">{formatDay(item.dayOfWeek)}</strong>
              {item.closed ? (
                <div className="col-span-2 flex items-center text-sm font-semibold text-slate-400">
                  Ngày nghỉ
                </div>
              ) : (
                <>
                  <input
                    type="time"
                    value={item.openTime.slice(0, 5)}
                    onChange={(event) =>
                      setSchedule((all) =>
                        all.map((value, i) =>
                          i === index
                            ? { ...value, openTime: `${event.target.value}:00` }
                            : value,
                        ),
                      )
                    }
                    className="input-field"
                  />
                  <input
                    type="time"
                    value={item.closeTime.slice(0, 5)}
                    onChange={(event) =>
                      setSchedule((all) =>
                        all.map((value, i) =>
                          i === index
                            ? { ...value, closeTime: `${event.target.value}:00` }
                            : value,
                        ),
                      )
                    }
                    className="input-field"
                  />
                </>
              )}
              <label className="flex items-center gap-2 text-sm">
                <input
                  type="checkbox"
                  checked={item.closed}
                  onChange={(event) =>
                    setSchedule((all) =>
                      all.map((value, i) =>
                        i === index
                          ? { ...value, closed: event.target.checked }
                          : value,
                      ),
                    )
                  }
                />{" "}
                Đóng
              </label>
            </div>
          ))}
        </div>
      </section>
      {update.isSuccess ? (
        <p className="rounded-xl bg-sky-50 p-3 text-sm text-sky-700">
          Đã lưu thay đổi.
        </p>
      ) : null}
      {update.error ? (
        <p className="rounded-xl bg-rose-50 p-3 text-sm text-rose-700">
          {update.error.message}
        </p>
      ) : null}
      <button
        disabled={update.isPending}
        className="inline-flex items-center gap-2 rounded-full bg-sky-500 px-6 py-3 text-sm font-black text-white hover:bg-sky-600 shadow-none disabled:opacity-50"
      >
        {update.isPending ? (
          <LoaderCircle className="size-4 animate-spin" />
        ) : (
          <Save className="size-4" />
        )}{" "}
        Lưu thay đổi
      </button>
    </form>
  );
}
function FieldLabel({
  label,
  children,
}: {
  label: string;
  children: React.ReactNode;
}) {
  return (
    <label>
      <span className="mb-2 block text-sm font-bold">{label}</span>
      {children}
    </label>
  );
}
