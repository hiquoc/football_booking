"use client";

import { useToggleFavoriteField } from "@/lib/hooks/use-fields";
import { FieldBookmarkButton } from "@/components/ui/field-bookmark-button";

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
    <FieldBookmarkButton
      saved={saved}
      disabled={toggleFavorite.isPending}
      onClick={(event) => {
        event.preventDefault();
        event.stopPropagation();
        toggleFavorite.mutate(!saved);
      }}
      className={className}
    />
  );
}
