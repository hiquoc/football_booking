"use client";

import { useState } from "react";
import { LoaderCircle, Send, Star } from "lucide-react";
import { useCreateReview } from "@/lib/hooks/use-fields";

export function ReviewForm({ fieldId }: { fieldId: string }) {
  const [rating, setRating] = useState(5);
  const [comment, setComment] = useState("");
  const mutation = useCreateReview(fieldId);
  async function submit(event: React.FormEvent) {
    event.preventDefault();
    try {
      await mutation.mutateAsync({
        rating,
        comment: comment.trim() || undefined,
      });
      setComment("");
    } catch {
      /* Rendered below. */
    }
  }
  return (
    <form
      onSubmit={submit}
      className="mt-6 rounded-2xl border border-slate-200 bg-white p-5 shadow-sm"
    >
      <h3 className="font-black text-slate-900">Chia sẻ trải nghiệm của bạn</h3>
      <div className="mt-3 flex gap-1" aria-label="Chọn số sao">
        {[1, 2, 3, 4, 5].map((value) => (
          <button
            key={value}
            type="button"
            onClick={() => setRating(value)}
            aria-label={`${value} sao`}
            className="rounded-full p-1 hover:bg-amber-50 hover:scale-110"
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
          Cảm ơn bạn đã đánh giá.
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
        disabled={mutation.isPending}
        className="mt-4 inline-flex items-center gap-2 rounded-xl bg-green-600 px-5 py-2.5 text-sm font-black text-white hover:bg-green-700"
      >
        {mutation.isPending ? (
          <LoaderCircle className="size-4 animate-spin" />
        ) : (
          <Send className="size-4" />
        )}{" "}
        Gửi đánh giá
      </button>
    </form>
  );
}
