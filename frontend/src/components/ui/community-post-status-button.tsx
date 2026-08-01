import type { CommunityApplicationStatus, CommunityPostStatus } from "@/lib/api/types";
import { applicationStatusLabels, postStatusLabels } from "@/components/community/community-labels";

type Status = CommunityPostStatus | CommunityApplicationStatus;

const statusClassName: Record<Status, string> = {
  OPEN: "bg-green-600 text-white",
  MATCHED: "bg-green-600 text-white",
  FULL: "bg-green-600 text-white",
  CLOSED: "bg-slate-500 text-white",
  CANCELLED: "bg-rose-500 text-white",
  HIDDEN: "bg-slate-500 text-white",
  PENDING: "bg-amber-500 text-white",
  ACCEPTED: "bg-green-600 text-white",
  REJECTED: "bg-rose-500 text-white",
  WITHDRAWN: "bg-slate-500 text-white",
};

export function CommunityPostStatusButton({
  status,
  size = "md",
  className = "",
}: {
  status: Status;
  size?: "sm" | "md" | "lg";
  className?: string;
}) {
  const label = status in postStatusLabels
    ? postStatusLabels[status as CommunityPostStatus]
    : applicationStatusLabels[status as CommunityApplicationStatus];
  const sizeClassName =
    size === "lg"
      ? "h-12 rounded-xl px-5 text-base leading-none"
      : size === "sm"
        ? "h-8 rounded-lg px-3 text-xs leading-none"
        : "h-10 rounded-lg px-4 text-sm leading-none";

  return (
    <span
      className={`inline-flex items-center justify-center font-black shadow-sm ${sizeClassName} ${statusClassName[status]} ${className}`}
      aria-label={label}
    >
      {label}
    </span>
  );
}
