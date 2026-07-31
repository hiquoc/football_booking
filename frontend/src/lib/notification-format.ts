import type { Notification } from "@/lib/api/types";
import { formatCurrency } from "./field-format";

function text(payload: Record<string, unknown>, key: string) {
  const value = payload[key];
  return typeof value === "string" || typeof value === "number" ? String(value) : null;
}

function joinParts(parts: Array<string | false | null | undefined>) {
  return parts.filter(Boolean).join(" · ");
}

function bookingDetails(notification: Notification) {
  const field = text(notification.payload, "fieldName");
  const date = text(notification.payload, "bookingDate");
  const start = text(notification.payload, "startTime")?.slice(0, 5);
  const end = text(notification.payload, "endTime")?.slice(0, 5);
  const amount = Number(notification.payload.totalAmount);
  return joinParts([
    field,
    date && start && end ? `${date}, ${start} - ${end}` : null,
    Number.isFinite(amount) ? formatCurrency(amount) : null,
  ]);
}

function communityDetails(notification: Notification) {
  const field = text(notification.payload, "fieldName");
  const date = text(notification.payload, "bookingDate");
  const start = text(notification.payload, "startTime")?.slice(0, 5);
  return joinParts([field, date && start ? `${date}, ${start}` : null]);
}

export function formatNotification(notification: Notification) {
  const booking = bookingDetails(notification);
  const community = communityDetails(notification);
  const amount = Number(notification.payload.amount);
  const reason = text(notification.payload, "reason");
  const code = text(notification.payload, "bookingCode");

  switch (notification.code) {
    case "BOOKING_CREATED":
      return { title: "Đã tạo yêu cầu đặt sân", detail: booking };
    case "BOOKING_CONFIRMED":
      return { title: "Đặt sân đã được xác nhận", detail: booking };
    case "BOOKING_CANCELLED":
      return {
        title: "Đặt sân đã bị hủy",
        detail: joinParts([booking, reason && `Lý do: ${reason}`]),
      };
    case "PAYMENT_SUCCESS":
      return {
        title: "Thanh toán thành công",
        detail: joinParts([code && `Mã ${code}`, Number.isFinite(amount) && formatCurrency(amount)]),
      };
    case "PAYMENT_FAILED":
      return {
        title: "Thanh toán thất bại",
        detail: joinParts([code && `Mã ${code}`, reason && `Lý do: ${reason}`]),
      };
    case "WALLET_TOP_UP_SUCCEEDED":
      return {
        title: "Nap vi thanh cong",
        detail: joinParts([Number.isFinite(amount) && formatCurrency(amount), code && `Ma ${code}`]),
      };
    case "COMMUNITY_POST_APPLIED":
      return { title: "Có người vừa ứng tuyển", detail: community };
    case "COMMUNITY_APPLICATION_WITHDRAWN":
      return { title: "Ứng viên đã rút yêu cầu", detail: community };
    case "COMMUNITY_APPLICATION_ACCEPTED":
      return { title: "Yêu cầu tham gia đã được chấp nhận", detail: community };
    case "COMMUNITY_APPLICATION_REJECTED":
      return { title: "Yêu cầu tham gia đã bị từ chối", detail: community };
    case "COMMUNITY_OPPONENT_MATCHED":
      return { title: "Đội của bạn đã được chấp nhận", detail: community };
    case "COMMUNITY_PLAYER_RECRUITMENT_FULL":
      return { title: "Đã tuyển đủ người cho trận đấu", detail: community };
    case "COMMUNITY_POST_CLOSED":
      return { title: "Bài đăng đã đóng", detail: community };
    default:
      return { title: notification.title, detail: reason ?? "" };
  }
}
