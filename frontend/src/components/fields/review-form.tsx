"use client";

import { useState } from "react";
import { LoaderCircle, Send, Star } from "lucide-react";
import { useCreateReview, useMyFieldReview } from "@/lib/hooks/use-fields";

export function ReviewForm({ fieldId }: { fieldId: string }) {
  const myReview = useMyFieldReview(fieldId);
  return (
    <ReviewFormFields
      key={myReview.data?.id ?? "new-review"}
      fieldId={fieldId}
      initialRating={myReview.data?.rating ?? 5}
      initialComment={myReview.data?.comment ?? ""}
      hasExistingReview={Boolean(myReview.data)}
      isReviewFetching={myReview.isFetching}
    />
  );
}

function ReviewFormFields({
  fieldId,
  initialRating,
  initialComment,
  hasExistingReview,
  isReviewFetching,
}: {
  fieldId: string;
  initialRating: number;
  initialComment: string;
  hasExistingReview: boolean;
  isReviewFetching: boolean;
}) {
  const [rating, setRating] = useState(initialRating);
  const [comment, setComment] = useState(initialComment);
  const mutation = useCreateReview(fieldId);

  async function submit(event: React.FormEvent) {
    event.preventDefault();
    try {
      await mutation.mutateAsync({
        rating,
        comment: comment.trim() || undefined,
      });
    } catch {
      /* Rendered below. */
    }
  }

  const loading = mutation.isPending || isReviewFetching;

  return (
    <form
      onSubmit={submit}
      className="mt-6 rounded-2xl border border-slate-200 bg-white p-5 shadow-sm"
    >
      <h3 className="font-black text-slate-900">
        {hasExistingReview ? "Cập nhật đánh giá của bạn" : "Chia sẻ trải nghiệm của bạn"}
      </h3>
      <div className="mt-3 flex gap-1" aria-label="Chọn số sao">
        {[1, 2, 3, 4, 5].map((value) => (
          <button
            key={value}
            type="button"
            onClick={() => setRating(value)}
            aria-label={`${value} sao`}
            className="rounded-full p-1 hover:scale-110 hover:bg-amber-50"
          >
            <Star
              className={`size-6 ${value <= rating ? "fill-amber-400 text-amber-400" : "text-slate-200"}`}
            />
          </button>
        ))}
      </div>
      <textarea
        value={comment}
        onChange={(event) => setComment(event.target.value)}
        rows={3}
        maxLength={1000}
        className="input-field mt-4 resize-none"
        placeholder="Sân, dịch vụ và trải nghiệm thi đấu thế nào?"
      />
      {mutation.isSuccess ? (
        <p className="mt-3 text-sm font-semibold text-green-700">
          {hasExistingReview ? "Đã cập nhật đánh giá của bạn." : "Cảm ơn bạn đã đánh giá."}
        </p>
      ) : null}
      {mutation.error ? (
        <p className="mt-3 text-sm text-rose-700">
          {mutation.error.message === "Phiên đăng nhập đã hết hạn"
            ? "Vui lòng đăng nhập để gửi đánh giá."
            : mutation.error.message}
        </p>
      ) : null}
      <button
        disabled={loading}
        className="mt-4 inline-flex items-center gap-2 rounded-xl bg-green-600 px-5 py-2.5 text-sm font-black text-white hover:bg-green-700 disabled:cursor-not-allowed disabled:opacity-60"
      >
        {loading ? (
          <LoaderCircle className="size-4 animate-spin" />
        ) : (
          <Send className="size-4" />
        )}{" "}
        {hasExistingReview ? "Cập nhật đánh giá" : "Gửi đánh giá"}
      </button>
    </form>
  );
}
