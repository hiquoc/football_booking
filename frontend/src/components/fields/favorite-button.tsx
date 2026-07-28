"use client";

import { useToggleFavoriteField } from "@/lib/hooks/use-fields";
import { Bookmark } from "lucide-react";

export function FavoriteButton({
  fieldId,
  isSaved,
  isFavorite,
  className,
}: {
  fieldId: string;
  isSaved?: boolean;
  isFavorite?: boolean;
  className?: string;
}) {
  const saved = Boolean(isSaved ?? isFavorite);
  const toggleFavorite = useToggleFavoriteField(fieldId);

  return (
    <button
      type="button"
      disabled={toggleFavorite.isPending}
      onClick={(event) => {
        event.preventDefault();
        event.stopPropagation();
        toggleFavorite.mutate(!saved);
      }}
      aria-label={saved ? "Remove saved field" : "Save field"}
      title={saved ? "Remove saved field" : "Save field"}
      className={
        className ??
        "inline-grid size-11 place-items-center rounded-full border border-white/80 bg-white/95 text-xl shadow-lg transition hover:scale-105 disabled:cursor-wait disabled:opacity-70"
      }
    >
      <span aria-hidden="true"><Bookmark className="size-5 text-sky-600" fill={saved ? "currentColor" : "none"} /></span>
    </button>
  );
}
