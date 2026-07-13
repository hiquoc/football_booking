"use client";

import { useToggleFavoriteField } from "@/lib/hooks/use-fields";

export function FavoriteButton({
  fieldId,
  isFavorite,
  className,
}: {
  fieldId: string;
  isFavorite?: boolean;
  className?: string;
}) {
  const favorite = Boolean(isFavorite);
  const toggleFavorite = useToggleFavoriteField(fieldId);

  return (
    <button
      type="button"
      disabled={toggleFavorite.isPending}
      onClick={(event) => {
        event.preventDefault();
        event.stopPropagation();
        toggleFavorite.mutate(!favorite);
      }}
      aria-label={favorite ? "Bo khoi yeu thich" : "Them vao yeu thich"}
      title={favorite ? "Bo khoi yeu thich" : "Them vao yeu thich"}
      className={
        className ??
        "inline-grid size-11 place-items-center rounded-full border border-white/80 bg-white/95 text-xl shadow-lg transition hover:scale-105 disabled:cursor-wait disabled:opacity-70"
      }
    >
      <span aria-hidden="true">{favorite ? "❤️" : "♡"}</span>
    </button>
  );
}
