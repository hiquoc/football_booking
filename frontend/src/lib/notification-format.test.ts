import { describe, expect, it } from "vitest";
import type { Notification } from "./api/types";
import { formatNotification } from "./notification-format";

const base: Notification = {
  id: "1", userId: "user", code: "BOOKING_CANCELLED", title: "Cancelled",
  payload: { fieldName: "Sân A", bookingDate: "2026-07-04", startTime: "16:00:00", endTime: "17:30:00", reason: "Mưa lớn" },
  isRead: false, createdAt: "2026-07-03T00:00:00", readAt: null,
};

describe("formatNotification", () => {
  it("shows booking schedule and cancellation reason", () => {
    const result = formatNotification(base);
    expect(result.detail).toContain("Sân A");
    expect(result.detail).toContain("16:00 - 17:30");
    expect(result.detail).toContain("Lý do: Mưa lớn");
  });
});
