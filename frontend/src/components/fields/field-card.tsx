"use client";

import Image from "next/image";
import Link from "next/link";
import { MapPin, Star } from "lucide-react";
import { useState } from "react";
import type { FieldCardData } from "@/lib/api/types";
import { formatFieldAddress, formatFieldType } from "@/lib/field-format";
import { FavoriteButton } from "./favorite-button";

export function FieldCard({
  field,
  canFavorite = false,
}: {
  field: FieldCardData;
  canFavorite?: boolean;
}) {
  const imageUrl = field.primaryImageUrl;
  const sportNames = field.fieldTypes
    .map((type) => formatFieldType(type))
    .sort((a, b) => a === "Bóng đá" ? -1 : b === "Bóng đá" ? 1 : 0);
  const [imageError, setImageError] = useState(false);

  return (
    <article className="group relative overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-[0_12px_32px_rgba(15,23,42,0.05)] transition duration-300 hover:-translate-y-1 hover:border-green-300 hover:shadow-[0_18px_42px_rgba(15,23,42,0.10)]">
      <div className="relative aspect-[16/10] overflow-hidden bg-[linear-gradient(135deg,#d1fae5,#99f6e4)]">
        {imageUrl && !imageError ? (
            <Image
              src={imageUrl}
              alt={`Hình ảnh ${field.name}`}
              fill
              sizes="(max-width: 768px) 100vw, (max-width: 1200px) 50vw, 33vw"
              className="object-cover bg-white transition duration-500 group-hover:scale-105"
              onError={() => setImageError(true)}
            />
        ) : (
          <div className="absolute inset-0 grid place-items-center field-pattern">
            <span className="rounded-full border border-green-950/10 bg-white/70 px-4 py-2 text-xs font-black uppercase tracking-[0.16em] text-green-900 backdrop-blur">
              {imageError ? "Lỗi hình ảnh" : "Chưa có hình ảnh"}
            </span>
          </div>
        )}
        {sportNames?.length > 0 && (
          <div className="pointer-events-none absolute left-4 top-4 z-30 flex gap-2">
            {sportNames.slice(0, sportNames.length >= 3 ? 2 : 3).map((sportName) => (
              <span
                key={sportName}
                className="rounded-full bg-green-100/95 px-3 py-1.5 text-[11px] font-black uppercase tracking-[0.12em] text-green-700 backdrop-blur"
              >
                {sportName}
              </span>
            ))}
            {sportNames.length >= 3 ? (
              <div className="group/types pointer-events-auto relative">
                <button
                  type="button"
                  className="rounded-full bg-slate-900/90 px-3 py-1.5 text-[11px] font-black tracking-[0.12em] text-white backdrop-blur"
                  aria-label="Xem tất cả loại sân"
                >
                  ...
                </button>
                <div className="invisible absolute left-0 top-full mt-2 w-max min-w-40 translate-y-1 rounded-xl border border-slate-200 bg-white p-3 opacity-0 shadow-xl transition group-hover/types:visible group-hover/types:translate-y-0 group-hover/types:opacity-100 group-focus-within/types:visible group-focus-within/types:translate-y-0 group-focus-within/types:opacity-100">
                  <p className="mb-2 text-[10px] font-black uppercase tracking-[0.12em] text-slate-400">
                    Loại sân
                  </p>
                  <div className="flex max-w-64 flex-wrap gap-1.5">
                    {sportNames.map((sportName) => (
                      <span
                        key={sportName}
                        className="rounded-full bg-green-50 px-2.5 py-1 text-xs font-bold text-green-700"
                      >
                        {sportName}
                      </span>
                    ))}
                  </div>
                </div>
              </div>
            ) : null}
          </div>
        )}
        {canFavorite ? (
          <div className="absolute right-4 top-4 z-40">
            <FavoriteButton fieldId={field.id} isSaved={field.isSaved ?? field.isFavorite} />
          </div>
        ) : null}
      </div>
      <div className="p-5">
        <h3 className="text-xl font-black leading-7 text-slate-950">
            <Link
              href={`/fields/${field.id}`}
              className="after:absolute after:inset-0"
            >
              {field.name}
            </Link>
        </h3>
        <p className="mt-3 flex items-start gap-2 text-sm leading-6 text-slate-500">
          <MapPin
            className="mt-1 size-4 shrink-0 text-green-600"
            aria-hidden="true"
          />
          <span className="line-clamp-2">{formatFieldAddress(field)}</span>
        </p>
        <div className="mt-5 flex items-center gap-2 text-sm">
          <span className="inline-flex items-center gap-1 font-black text-slate-800">
            <Star
              className="size-4 fill-amber-400 text-amber-400"
              aria-hidden="true"
            />
            {Number(field.ratingAverage ?? 0).toFixed(1)}
          </span>
          <span className="font-semibold text-slate-400">/</span>
          <span className="font-semibold text-slate-500">
            {field.totalReviews ?? 0} đánh giá
          </span>
        </div>
        <div className="relative mt-5 flex items-center justify-end border-t border-slate-100 pt-4">
          <Link
            href={`/fields/${field.id}`}
            className="relative z-10 inline-flex min-h-10 items-center justify-center rounded-xl bg-green-600 px-4 text-sm font-black text-white hover:bg-green-700"
          >
            Xem chi tiết
          </Link>
        </div>
      </div>
    </article>
  );
}
