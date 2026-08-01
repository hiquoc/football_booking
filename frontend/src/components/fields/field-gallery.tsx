"use client";

import Image from "next/image";
import { useEffect, useMemo, useState } from "react";
import { createPortal } from "react-dom";
import { ChevronLeft, ChevronRight, X } from "lucide-react";
import type { FieldImage } from "@/lib/api/types";

export function FieldGallery({
  images,
  name,
}: {
  images: FieldImage[];
  name: string;
}) {
  const [errorImages, setErrorImages] = useState<Set<number>>(new Set());
  const [activeIndex, setActiveIndex] = useState<number | null>(null);
  const sorted = useMemo(
    () => [...(images ?? [])].sort((a, b) => a.displayOrder - b.displayOrder),
    [images],
  );
  const visibleImages = sorted.slice(0, Math.min(sorted.length, 5));

  if (!sorted.length) {
    return (
      <div className="field-pattern grid aspect-[16/7] place-items-center rounded-2xl border border-slate-200 bg-green-50 text-sm font-black uppercase tracking-[0.15em] text-green-800">
        Chưa có hình ảnh
      </div>
    );
  }

  return (
    <>
      <div className={galleryClassName(visibleImages.length)}>
        {visibleImages.map((image, index) => (
          <button
            key={image.id}
            type="button"
            onClick={() => setActiveIndex(index)}
            className={`group relative min-h-32 overflow-hidden bg-slate-100 text-left ${tileClassName(visibleImages.length, index)}`}
            aria-label={`Xem hình ${index + 1} của ${name}`}
          >
            {errorImages.has(image.id) ? (
              <div className="absolute inset-0 grid place-items-center bg-green-50 field-pattern">
                <span className="rounded-full border border-green-950/10 bg-white/70 px-4 py-2 text-xs font-black uppercase tracking-[0.16em] text-green-900 backdrop-blur">
                  Lỗi hình ảnh
                </span>
              </div>
            ) : (
              <Image
                src={image.imageUrl}
                alt={`${name} - hình ${index + 1}`}
                fill
                priority={index === 0}
                sizes={index === 0 ? "(max-width: 640px) 100vw, 50vw" : "25vw"}
                className="object-cover transition duration-500 group-hover:scale-105"
                onError={() => {
                  setErrorImages((prev) => {
                    const newSet = new Set(prev);
                    newSet.add(image.id);
                    return newSet;
                  });
                }}
              />
            )}
            {index === visibleImages.length - 1 && sorted.length > visibleImages.length ? (
              <span className="pointer-events-none absolute inset-0 grid place-items-center bg-slate-950/55 text-xl font-black text-white backdrop-blur-[2px]">
                +{sorted.length - visibleImages.length} ảnh
              </span>
            ) : null}
          </button>
        ))}
      </div>
      {activeIndex !== null ? (
        <GalleryViewer
          images={sorted}
          activeIndex={activeIndex}
          name={name}
          onClose={() => setActiveIndex(null)}
          onChange={setActiveIndex}
        />
      ) : null}
    </>
  );
}

function galleryClassName(count: number) {
  const base = "grid overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-[0_14px_36px_rgba(15,23,42,0.08)]";
  if (count === 1) return `${base} aspect-[16/7]`;
  if (count === 2) return `${base} aspect-[16/9] gap-2 sm:aspect-[16/7] sm:grid-cols-2`;
  return `${base} aspect-[16/10] gap-2 sm:aspect-[16/7] sm:grid-cols-4 sm:grid-rows-2`;
}

function tileClassName(count: number, index: number) {
  if (count === 1 || count === 2) return "";
  if (index === 0) return "sm:col-span-2 sm:row-span-2";
  if (count === 3) return "sm:col-span-2";
  if (count === 4 && index === 3) return "sm:col-span-2";
  return "";
}

function GalleryViewer({
  images,
  activeIndex,
  name,
  onClose,
  onChange,
}: {
  images: FieldImage[];
  activeIndex: number;
  name: string;
  onClose: () => void;
  onChange: (index: number) => void;
}) {
  const image = images[activeIndex];
  const canNavigate = images.length > 1;

  useEffect(() => {
    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = "hidden";

    function handleKeydown(event: KeyboardEvent) {
      if (event.key === "Escape") onClose();
      if (event.key === "ArrowLeft") {
        onChange(previousIndex(activeIndex, images.length));
      }
      if (event.key === "ArrowRight") {
        onChange(nextIndex(activeIndex, images.length));
      }
    }

    window.addEventListener("keydown", handleKeydown);
    return () => {
      document.body.style.overflow = previousOverflow;
      window.removeEventListener("keydown", handleKeydown);
    };
  }, [activeIndex, images.length, onChange, onClose]);

  return createPortal(
    <div
      className="fixed inset-0 z-[100] grid place-items-center bg-slate-950/92 p-4 sm:p-8"
      role="dialog"
      aria-modal="true"
      aria-label={`Xem hình ảnh của ${name}`}
      onClick={onClose}
    >
      <button
        type="button"
        className="absolute right-4 top-4 z-50 grid size-11 place-items-center rounded-full bg-white/10 text-white transition hover:bg-white/20"
        onClick={onClose}
        aria-label="Đóng hình ảnh"
      >
        <X className="size-6" />
      </button>

      {canNavigate ? (
        <>
          <button
            type="button"
            className="absolute left-4 top-1/2 z-50 grid size-11 -translate-y-1/2 place-items-center rounded-full bg-white/12 text-white transition hover:bg-white/25"
            onClick={(event) => {
              event.stopPropagation();
              onChange(previousIndex(activeIndex, images.length));
            }}
            aria-label="Xem hình trước"
          >
            <ChevronLeft className="size-7" />
          </button>
          <button
            type="button"
            className="absolute right-4 top-1/2 z-50 grid size-11 -translate-y-1/2 place-items-center rounded-full bg-white/12 text-white transition hover:bg-white/25"
            onClick={(event) => {
              event.stopPropagation();
              onChange(nextIndex(activeIndex, images.length));
            }}
            aria-label="Xem hình tiếp theo"
          >
            <ChevronRight className="size-7" />
          </button>
        </>
      ) : null}

      <div
        className="relative h-full w-full"
        onClick={(event) => event.stopPropagation()}
      >
        <Image
          src={image.imageUrl}
          alt={`${name} - hình ${activeIndex + 1}`}
          fill
          sizes="100vw"
          className="object-contain"
          priority
        />
      </div>
      <p className="absolute bottom-4 left-1/2 z-50 -translate-x-1/2 rounded-full bg-white/12 px-4 py-2 text-sm font-bold text-white backdrop-blur">
        Hình {activeIndex + 1} / {images.length}
      </p>
    </div>,
    document.body,
  );
}

function previousIndex(index: number, total: number) {
  return (index - 1 + total) % total;
}

function nextIndex(index: number, total: number) {
  return (index + 1) % total;
}
