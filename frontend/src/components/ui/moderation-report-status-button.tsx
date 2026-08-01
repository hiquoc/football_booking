import type { CommunityReportStatus } from "@/lib/api/types";

const moderationReportStatusLabels: Record<CommunityReportStatus, string> = {
  PENDING: "Đang chờ",
  REVIEWED: "Đã duyệt",
};

const moderationReportStatusClassName: Record<CommunityReportStatus, string> = {
  PENDING: "bg-amber-500 text-white",
  REVIEWED: "bg-green-600 text-white",
};

export function ModerationReportStatusButton({
  status,
  size = "md",
  className = "",
}: {
  status: CommunityReportStatus;
  size?: "sm" | "md" | "lg";
  className?: string;
}) {
  const sizeClassName =
    size === "lg"
      ? "h-12 rounded-xl px-5 text-base leading-none"
      : size === "sm"
        ? "h-8 rounded-lg px-3 text-xs leading-none"
        : "h-10 rounded-lg px-4 text-sm leading-none";

  return (
    <span
      className={`inline-flex items-center justify-center font-black shadow-sm ${sizeClassName} ${moderationReportStatusClassName[status]} ${className}`}
      aria-label={moderationReportStatusLabels[status]}
    >
      {moderationReportStatusLabels[status]}
    </span>
  );
}
