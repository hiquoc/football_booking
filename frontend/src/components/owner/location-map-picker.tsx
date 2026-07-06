"use client";

import dynamic from "next/dynamic";

const LeafletLocationMap = dynamic(() => import("./leaflet-location-map"), {
  ssr: false,
  loading: () => (
    <div className="grid min-h-80 place-items-center bg-slate-100 text-sm font-semibold text-slate-500">
      Đang tải bản đồ…
    </div>
  ),
});

export function LocationMapPicker({
  latitude,
  longitude,
  onChange,
}: {
  latitude: number | null;
  longitude: number | null;
  onChange: (latitude: number, longitude: number) => void;
}) {
  const hasPosition = latitude !== null && longitude !== null;

  return (
    <div>
      <p className="mb-3 text-sm text-slate-500">
        Nhấp vào bản đồ hoặc kéo ghim để chọn vị trí chính xác của sân.
      </p>
      <div className="overflow-hidden rounded-2xl border border-slate-200">
        <LeafletLocationMap
          latitude={latitude}
          longitude={longitude}
          onChange={onChange}
        />
      </div>
      <input type="hidden" name="latitude" value={latitude ?? ""} />
      <input type="hidden" name="longitude" value={longitude ?? ""} />
      <div
        className="mt-3 grid gap-3 text-sm sm:grid-cols-2"
        aria-live="polite"
      >
        <p className="rounded-xl bg-slate-50 px-4 py-3 text-slate-600">
          <strong>Vĩ độ:</strong> {hasPosition ? latitude.toFixed(6) : "Chưa chọn"}
        </p>
        <p className="rounded-xl bg-slate-50 px-4 py-3 text-slate-600">
          <strong>Kinh độ:</strong>{" "}
          {hasPosition ? longitude.toFixed(6) : "Chưa chọn"}
        </p>
      </div>
    </div>
  );
}
