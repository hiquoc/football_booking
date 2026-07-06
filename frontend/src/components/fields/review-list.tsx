import { Star } from "lucide-react";
import type { Review } from "@/lib/api/types";
import { formatDate } from "@/lib/field-format";

export function ReviewList({ reviews }: { reviews: Review[] | null }) {
  return (
    <section className="border-t border-slate-200 pt-10">
      <p className="text-xs font-black uppercase tracking-[0.18em] text-sky-600">
        Cảm nhận người chơi
      </p>
      <h2 className="mt-2 text-3xl font-black tracking-[-0.04em] text-slate-950">
        Đánh giá gần đây
      </h2>
      {!reviews ? (
        <Notice text="Không thể tải đánh giá." />
      ) : reviews.length === 0 ? (
        <Notice text="Chưa có đánh giá nào cho sân này." />
      ) : (
        <div className="mt-6 grid gap-4 md:grid-cols-2">
          {reviews.map((review) => (
            <article
              key={review.id}
              className="rounded-[1.5rem] border border-slate-200 bg-white p-5"
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
              <p className="mt-4 text-xs font-bold uppercase tracking-[0.12em] text-slate-400">
                Người chơi PitchUp
              </p>
            </article>
          ))}
        </div>
      )}
    </section>
  );
}

function Notice({ text }: { text: string }) {
  return (
    <p className="mt-6 rounded-2xl border border-dashed border-slate-300 bg-white p-8 text-center text-sm text-slate-500">
      {text}
    </p>
  );
}
