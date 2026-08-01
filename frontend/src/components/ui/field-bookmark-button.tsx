import type { MouseEventHandler } from "react";
import { Bookmark } from "lucide-react";

export function FieldBookmarkButton({
  saved,
  disabled,
  className = "",
  onClick,
}: {
  saved: boolean;
  disabled?: boolean;
  className?: string;
  onClick?: MouseEventHandler<HTMLButtonElement>;
}) {
  return (
    <button
      type="button"
      disabled={disabled}
      onClick={onClick}
      aria-label={saved ? "Bỏ lưu sân" : "Lưu sân"}
      title={saved ? "Bỏ lưu sân" : "Lưu sân"}
      className={`inline-grid size-11 place-items-center rounded-full border border-green-200 bg-white text-green-700 shadow-sm transition hover:border-green-300 hover:bg-green-50 disabled:cursor-wait disabled:opacity-70 ${className}`}
    >
      <Bookmark className="size-5" fill={saved ? "currentColor" : "none"} aria-hidden="true" />
    </button>
  );
}
