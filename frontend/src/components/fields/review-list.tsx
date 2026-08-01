"use client";

import { useState } from "react";
import Link from "next/link";
import { ChevronLeft, ChevronRight, LoaderCircle, Star } from "lucide-react";
import type { PageResponse, Review } from "@/lib/api/types";
import { formatDate } from "@/lib/field-format";
import { useFieldReviews } from "@/lib/hooks/use-fields";

const reviewPageSize = 6;

export function ReviewList({
  fieldId,
  initialReviews,
}: {
  fieldId: string;
  initialReviews: PageResponse<Review> | null;
}) {
  const [page, setPage] = useState(initialReviews?.page ?? 0);
  const reviews = useFieldReviews(
    fieldId,
    page,
    reviewPageSize,
    page === (initialReviews?.page ?? -1) ? initialReviews ?? undefined : undefined,
  );
  const data = reviews.data;

  return (
    <section className="border-t border-slate-200 pt-10">
      <div className="flex flex-col justify-between gap-3 sm:flex-row sm:items-end">
        <div>
          <p className="text-sm font-black uppercase text-green-600">
            Cảm nhận người chơi
          </p>
          <h2 className="mt-2 text-3xl font-black text-slate-950">
            Đánh giá gần đây
          </h2>
        </div>
        {data && data.totalPages > 1 ? (
          <p className="text-sm font-bold text-slate-500">
            Trang {data.page + 1}/{data.totalPages}
          </p>
        ) : null}
      </div>

      {reviews.isPending ? <ReviewSkeleton /> : null}
      {!reviews.isPending && reviews.isError ? (
        <Notice text="Không thể tải đánh giá." />
      ) : null}
      {!reviews.isPending && data?.empty ? (
        <Notice text="Chưa có đánh giá nào cho sân này." />
      ) : null}
      {!reviews.isPending && data?.content?.length ? (
        <>
          <div className="mt-6 grid gap-4 md:grid-cols-2">
            {data.content.map((review) => {
              const reviewerName = review.fullName?.trim() || "Người dùng PitchUp";

              return (
                <article
                  key={review.id}
                  className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm"
                >
                  <div className="flex items-center justify-between gap-4">
                    <span
                      className="flex gap-0.5"
                      aria-label={`${review.rating} trên 5 sao`}
                    >
                      {[1, 2, 3, 4, 5].map((star) => (
                        <Star
                          key={star}
                          className={`size-4 ${star <= review.rating ? "fill-amber-400 text-amber-400" : "text-slate-200"}`}
                        />
                      ))}
                    </span>
                    <time
                      className="text-xs font-semibold text-slate-400"
                      dateTime={review.createdAt}
                    >
                      {formatDate(review.createdAt)}
                    </time>
                  </div>
                  <p className="mt-4 text-sm leading-6 text-slate-600">
                    {review.comment ||
                      "Người chơi đã đánh giá sân nhưng không để lại nhận xét."}
                  </p>
                  <Link
                    href={`/users/${review.userId}/profile`}
                    className="mt-4 inline-flex text-xs font-black uppercase tracking-[0.12em] text-green-700 hover:text-green-800"
                  >
                    {reviewerName}
                  </Link>
                </article>
              );
            })}
          </div>
          <ReviewPagination
            page={data.page}
            totalPages={data.totalPages}
            isLoading={reviews.isFetching}
            onPageChange={setPage}
          />
        </>
      ) : null}
    </section>
  );
}

function ReviewPagination({
  page,
  totalPages,
  isLoading,
  onPageChange,
}: {
  page: number;
  totalPages: number;
  isLoading: boolean;
  onPageChange: (page: number) => void;
}) {
  if (totalPages <= 1) return null;

  return (
    <div className="mt-6 flex flex-wrap items-center justify-center gap-3">
      <button
        type="button"
        disabled={page === 0 || isLoading}
        onClick={() => onPageChange(Math.max(0, page - 1))}
        className="inline-flex min-h-10 items-center gap-2 rounded-xl border border-slate-200 bg-white px-4 text-sm font-black text-slate-700 disabled:cursor-not-allowed disabled:opacity-50"
      >
        <ChevronLeft className="size-4" />
        Trước
      </button>
      <span className="inline-flex min-h-10 items-center rounded-xl bg-green-50 px-4 text-sm font-black text-green-700">
        {isLoading ? <LoaderCircle className="mr-2 size-4 animate-spin" /> : null}
        {page + 1}/{totalPages}
      </span>
      <button
        type="button"
        disabled={page + 1 >= totalPages || isLoading}
        onClick={() => onPageChange(page + 1)}
        className="inline-flex min-h-10 items-center gap-2 rounded-xl bg-green-600 px-4 text-sm font-black text-white disabled:cursor-not-allowed disabled:opacity-50"
      >
        Sau
        <ChevronRight className="size-4" />
      </button>
    </div>
  );
}

function ReviewSkeleton() {
  return (
    <div className="mt-6 grid animate-pulse gap-4 md:grid-cols-2">
      {Array.from({ length: 4 }).map((_, index) => (
        <div key={index} className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
          <div className="h-4 w-32 rounded bg-slate-200" />
          <div className="mt-5 h-4 w-full rounded bg-slate-200" />
          <div className="mt-3 h-4 w-2/3 rounded bg-slate-200" />
          <div className="mt-5 h-3 w-28 rounded bg-slate-200" />
        </div>
      ))}
    </div>
  );
}

function Notice({ text }: { text: string }) {
  return (
    <p className="mt-6 rounded-2xl border border-dashed border-slate-300 bg-white p-8 text-center text-sm text-slate-500">
      {text}
    </p>
  );
}
