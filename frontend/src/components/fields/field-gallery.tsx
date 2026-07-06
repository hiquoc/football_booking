"use client";

import Image from "next/image";
import { useState } from "react";
import type { FieldImage } from "@/lib/api/types";
import { ImageLightbox } from "@/components/ui/image-lightbox";

export function FieldGallery({
  images,
  name,
}: {
  images: FieldImage[];
  name: string;
}) {
  const [errorImages, setErrorImages] = useState<Set<number>>(new Set());
  const sorted = [...(images ?? [])]
    .sort(
      (a, b) =>a.displayOrder - b.displayOrder,
    );

  if (!sorted.length) {
    return (
      <div className="field-pattern grid aspect-[16/7] place-items-center rounded-[2rem] bg-sky-100 text-sm font-black uppercase tracking-[0.15em] text-sky-900">
        Chưa có hình ảnh
      </div>
    );
  }

  return (
    <div className="grid aspect-[16/10] gap-2 overflow-hidden rounded-[2rem] sm:aspect-[16/7] sm:grid-cols-4 sm:grid-rows-2">
      {sorted.slice(0, 5).map((image, index) => (
        <div
          key={image.id}
          className={`relative overflow-hidden bg-slate-200 ${index === 0 ? "sm:col-span-2 sm:row-span-2" : ""}`}
        >
          {errorImages.has(image.id) ? (
            <div className="absolute inset-0 grid place-items-center bg-sky-100 field-pattern">
              <span className="rounded-full border border-sky-950/10 bg-white/60 px-4 py-2 text-xs font-black uppercase tracking-[0.16em] text-sky-900 backdrop-blur">
                Lỗi hình ảnh
              </span>
            </div>
          ) : (
            <ImageLightbox
              src={image.imageUrl}
              alt={`${name} - hình ${index + 1}`}
              className="absolute inset-0 cursor-zoom-in"
            >
              <Image
                src={image.imageUrl}
                alt={`${name} - hình ${index + 1}`}
                fill
                priority={index === 0}
                sizes={index === 0 ? "(max-width: 640px) 100vw, 50vw" : "25vw"}
                className="object-cover transition duration-500 hover:scale-105"
                onError={() => {
                  setErrorImages((prev) => {
                    const newSet = new Set(prev);
                    newSet.add(image.id);
                    return newSet;
                  });
                }}
              />
            </ImageLightbox>
          )}
          {index === 4 && sorted.length > 5 ? (
            <span className="pointer-events-none absolute inset-0 grid place-items-center bg-sky-50/85 text-lg font-black text-slate-900 backdrop-blur-sm">
              +{sorted.length - 5} ảnh
            </span>
          ) : null}
        </div>
      ))}
    </div>
  );
}
