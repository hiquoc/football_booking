import type { BookingDisplayStatus } from "@/lib/booking-format";
import { getBookingStatus } from "@/lib/booking-format";

const statusButtonClassName: Record<BookingDisplayStatus, string> = {
  PENDING: "bg-amber-500 text-white",
  CONFIRMED: "bg-green-600 text-white",
  IN_PROGRESS: "bg-green-600 text-white",
  CANCELLED: "bg-rose-500 text-white",
  COMPLETED: "bg-green-600 text-white",
  REPORTED: "bg-amber-500 text-white",
  EXPIRED: "bg-slate-500 text-white",
};

export function BookingStatusButton({
  status,
  size = "md",
  className = "",
}: {
  status: BookingDisplayStatus;
  label?: string;
  size?: "sm" | "md" | "lg";
  className?: string;
}) {
  const meta = getBookingStatus(status);
  const sizeClassName =
    size === "lg"
      ? "min-h-12 rounded-xl px-5 py-3 text-base"
      : size === "sm"
        ? "min-h-8 rounded-lg px-3 py-1.5 text-xs"
        : "min-h-10 rounded-lg px-4 py-2 text-sm";

  return (
    <span
      className={`inline-flex items-center justify-center font-black shadow-sm ${sizeClassName} ${statusButtonClassName[status]} ${className}`}
      aria-label={meta.label}
    >
      {meta.label}
    </span>
  );
}
