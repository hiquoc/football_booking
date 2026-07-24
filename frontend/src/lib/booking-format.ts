import type { Booking, BookingStatus } from "@/lib/api/types";

export type BookingDisplayStatus = BookingStatus | "IN_PROGRESS";

export const bookingStatus: Record<
  BookingDisplayStatus,
  { label: string; className: string }
> = {
  PENDING: {
    label: "Chờ thanh toán",
    className: "bg-amber-100 text-amber-700",
  },
  CONFIRMED: {
    label: "Đã xác nhận",
    className: "bg-sky-100 text-sky-700",
  },
  IN_PROGRESS: {
    label: "Đang diễn ra",
    className: "bg-emerald-100 text-emerald-700",
  },
  CANCELLED: { label: "Đã hủy", className: "bg-rose-100 text-rose-700" },
  COMPLETED: { label: "Hoàn thành", className: "bg-sky-100 text-sky-700" },
  EXPIRED: { label: "Đã hết hạn", className: "bg-slate-100 text-slate-600" },
};

const unknownBookingStatus = {
  label: "Không xác định",
  className: "bg-slate-100 text-slate-600",
};

export function getBookingStatus(status: string) {
  return bookingStatus[status as BookingDisplayStatus] ?? unknownBookingStatus;
}

export function deriveBookingDisplayStatus(
  booking: Pick<Booking, "status" | "bookingDate" | "startTime" | "endTime" | "startDateTime" | "endDateTime">,
  now = new Date(),
): BookingDisplayStatus {
  if (booking.status !== "CONFIRMED") return booking.status;

  const start = new Date(booking.startDateTime ?? `${booking.bookingDate}T${booking.startTime}`);
  const end = new Date(booking.endDateTime ?? `${booking.bookingDate}T${booking.endTime}`);
  if (Number.isNaN(start.getTime()) || Number.isNaN(end.getTime())) return booking.status;
  if (now >= end) return "COMPLETED";
  if (now >= start) return "IN_PROGRESS";
  return booking.status;
}

export function formatBookingDate(value: string) {
  return new Intl.DateTimeFormat("vi-VN", {
    weekday: "long",
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
  }).format(new Date(`${value}T00:00:00`));
}

export function formatBookingDateTime(value: string) {
  return new Intl.DateTimeFormat("vi-VN", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  }).format(new Date(value));
}

export function bookingStartDateTime(booking: Pick<Booking, "bookingDate" | "startTime" | "startDateTime">) {
  return booking.startDateTime ?? `${booking.bookingDate}T${booking.startTime}`;
}

export function bookingEndDateTime(booking: Pick<Booking, "bookingDate" | "endTime" | "endDateTime">) {
  return booking.endDateTime ?? `${booking.bookingDate}T${booking.endTime}`;
}
