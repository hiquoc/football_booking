"use client";

import Image from "next/image";
import {
  ArrowLeft,
  ArrowRight,
  ImagePlus,
  LoaderCircle,
  Star,
  Trash2,
} from "lucide-react";
import type { FieldImage } from "@/lib/api/types";
import { ImageLightbox } from "@/components/ui/image-lightbox";
import { DataEmpty, DataError, ListSkeleton } from "@/components/ui/data-state";
import { useFieldBookingData } from "@/lib/hooks/use-fields";
import {
  useChangeFieldImageOrder,
  useDeleteFieldImage,
  useUploadFieldImages,
} from "@/lib/hooks/use-owner-fields";

function reorderImages(images: FieldImage[], fromIndex: number, toIndex: number) {
  if (
    fromIndex === toIndex ||
    fromIndex < 0 ||
    toIndex < 0 ||
    fromIndex >= images.length ||
    toIndex >= images.length
  ) {
    return images;
  }

  const next = [...images];
  const [moved] = next.splice(fromIndex, 1);
  next.splice(toIndex, 0, moved);
  return next;
}

export function ImageManager({ fieldId }: { fieldId: string }) {
  const field = useFieldBookingData(fieldId).field;
  const upload = useUploadFieldImages(fieldId);
  const remove = useDeleteFieldImage(fieldId);
  const updateOrder = useChangeFieldImageOrder(fieldId);

  if (field.isPending) return <ListSkeleton />;
  if (field.isError) return <DataError title="Không thể tải hình ảnh" />;

  const images = [...(field.data.images ?? [])].sort(
    (a, b) => a.displayOrder - b.displayOrder,
  );
  const submitImageOrder = (orderedImages: FieldImage[]) => {
    updateOrder.mutate(orderedImages.map((image) => image.id));
  };

  return (
    <div>
      <label className="mb-6 flex cursor-pointer items-center justify-center gap-2 rounded-xl border border-dashed border-slate-300 bg-white px-5 py-6 text-sm font-semibold text-slate-700 transition hover:border-green-400 hover:bg-green-50 hover:text-green-700">
        {upload.isPending ? (
          <LoaderCircle className="size-5 animate-spin" />
        ) : (
          <ImagePlus className="size-5" />
        )}{" "}
        {upload.isPending ? "Đang tải ảnh lên..." : "Chọn ảnh để tải lên"}
        <input
          type="file"
          accept="image/jpeg,image/png,image/webp"
          multiple
          disabled={upload.isPending || remove.isPending || updateOrder.isPending}
          className="sr-only"
          onChange={(event) => {
            if (event.target.files?.length) upload.mutate(event.target.files);
          }}
        />
      </label>
      {upload.error ? (
        <p className="mb-5 rounded-xl bg-rose-50 p-3 text-sm text-rose-700">
          {upload.error.message}
        </p>
      ) : null}
      {updateOrder.error ? (
        <p className="mb-5 rounded-xl bg-rose-50 p-3 text-sm text-rose-700">
          {updateOrder.error.message}
        </p>
      ) : null}
      {!images.length ? (
        <DataEmpty
          title="Chưa có hình ảnh"
          description="Tải ảnh chất lượng cao để địa điểm nổi bật hơn."
        />
      ) : (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {images.map((image, index) => (
            <article
              key={image.id}
              className="group overflow-hidden rounded-xl border border-slate-200 bg-white shadow-sm"
            >
              <div className="relative aspect-video bg-slate-100">
                <ImageLightbox
                  src={image.imageUrl}
                  alt={`Hình ảnh sân ${field.data.name}`}
                  className="absolute inset-0 cursor-zoom-in"
                >
                  <Image
                    src={image.imageUrl}
                    alt={`Hình ảnh sân ${field.data.name}`}
                    fill
                    className="object-cover"
                    sizes="(max-width: 640px) 100vw, 33vw"
                  />
                </ImageLightbox>
                {image.isPrimary ? (
                  <span className="absolute left-3 top-3 inline-flex items-center gap-1 rounded-full bg-amber-400 px-2.5 py-1 text-xs font-black text-slate-950">
                    <Star className="size-3 fill-current" /> Ảnh bìa
                  </span>
                ) : (
                  <button
                    onClick={() => submitImageOrder(reorderImages(images, index, 0))}
                    disabled={updateOrder.isPending}
                    className="absolute left-3 top-3 inline-flex h-6 items-center gap-1 rounded-full bg-white hover:bg-amber-400 px-2.5 py-1 !text-xs !font-bold text-slate-950 transition cursor-pointer disabled:cursor-not-allowed disabled:opacity-50">
                    <Star className="size-3 fill-current" />Làm ảnh bìa
                  </button>
                )}
                <span className="pointer-events-none absolute bottom-2 right-2 rounded-md bg-slate-950/70 px-2 py-1 text-xs font-medium text-white">
                  {index + 1}/{images.length}
                </span>
              </div>
              <div className="flex items-center justify-between gap-2 p-3">
                <div className="flex items-center gap-1">
                  <button
                    onClick={() =>
                      submitImageOrder(reorderImages(images, index, index - 1))
                    }
                    disabled={updateOrder.isPending || index === 0}
                    className="grid size-9 place-items-center rounded-full border border-slate-200 text-slate-600 transition hover:bg-slate-100 disabled:cursor-not-allowed disabled:opacity-35"
                    aria-label="Di chuyển sang trái"
                  >
                    <ArrowLeft className="size-4" />
                  </button>
                  <button
                    onClick={() =>
                      submitImageOrder(reorderImages(images, index, index + 1))
                    }
                    disabled={updateOrder.isPending || index === images.length - 1}
                    className="grid size-9 place-items-center rounded-full border border-slate-200 text-slate-600 transition hover:bg-slate-100 disabled:cursor-not-allowed disabled:opacity-35"
                    aria-label="Di chuyển sang phải"
                  >
                    <ArrowRight className="size-4" />
                  </button>
                  
                </div>
                <div className="flex justify-end">
                  <button
                    onClick={() => {
                      if (window.confirm("Bạn có chắc muốn xóa hình ảnh này?")) {
                        remove.mutate(image.id);
                      }
                    }}
                    disabled={remove.isPending || updateOrder.isPending}
                    className="ml-auto grid size-9 place-items-center rounded-full border border-rose-200 text-rose-600 hover:border-rose-300 hover:bg-rose-100 disabled:opacity-50"
                    aria-label="Xóa ảnh"
                  >
                    <Trash2 className="size-4" />
                  </button>
                </div>
              </div>
            </article>
          ))}
        </div>
      )}
    </div>
  );
}
