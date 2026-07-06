import type { Notification } from "@/lib/api/types";
import { formatCurrency } from "./field-format";

function text(payload: Record<string, unknown>, key: string) {
  const value = payload[key];
  return typeof value === "string" || typeof value === "number" ? String(value) : null;
}

function bookingDetails(notification: Notification) {
  const field = text(notification.payload, "fieldName");
  const date = text(notification.payload, "bookingDate");
  const start = text(notification.payload, "startTime")?.slice(0, 5);
  const end = text(notification.payload, "endTime")?.slice(0, 5);
  const amount = Number(notification.payload.totalAmount);
  const parts = [field, date && start && end ? `${date}, ${start} - ${end}` : null];
  if (Number.isFinite(amount)) parts.push(formatCurrency(amount));
  return parts.filter(Boolean).join(" · ");
}

export function formatNotification(notification: Notification) {
  const booking = bookingDetails(notification);
  const amount = Number(notification.payload.amount);
  const reason = text(notification.payload, "reason");
  const code = text(notification.payload, "bookingCode");

  switch (notification.code) {
    case "BOOKING_CREATED":
      return { title: "Đã tạo yêu cầu đặt sân", detail: booking };
    case "BOOKING_CONFIRMED":
      return { title: "Đặt sân đã được xác nhận", detail: booking };
    case "BOOKING_CANCELLED":
      return { title: "Đặt sân đã bị hủy", detail: [booking, reason && `Lý do: ${reason}`].filter(Boolean).join(" · ") };
    case "PAYMENT_SUCCESS":
      return { title: "Thanh toán thành công", detail: [code && `Mã ${code}`, Number.isFinite(amount) && formatCurrency(amount)].filter(Boolean).join(" · ") };
    case "PAYMENT_FAILED":
      return { title: "Thanh toán thất bại", detail: [code && `Mã ${code}`, reason && `Lý do: ${reason}`].filter(Boolean).join(" · ") };
    default:
      return { title: notification.title, detail: reason ?? "" };
  }
}
