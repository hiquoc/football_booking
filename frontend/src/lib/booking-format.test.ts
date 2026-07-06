import { describe, expect, it } from "vitest";
import { deriveBookingDisplayStatus, getBookingStatus } from "./booking-format";

const confirmedBooking = {
  status: "CONFIRMED" as const,
  bookingDate: "2026-07-05",
  startTime: "10:00:00",
  endTime: "11:00:00",
};

describe("booking display status", () => {
  it("formats expired unpaid bookings", () => {
    expect(getBookingStatus("EXPIRED")).toEqual({
      label: "Đã hết hạn",
      className: "bg-slate-100 text-slate-600",
    });
  });

  it("derives in-progress during the booked time", () => {
    expect(deriveBookingDisplayStatus(
      confirmedBooking,
      new Date("2026-07-05T10:30:00"),
    )).toBe("IN_PROGRESS");
  });

  it("derives completed as soon as the booked time ends", () => {
    expect(deriveBookingDisplayStatus(
      confirmedBooking,
      new Date("2026-07-05T11:00:00"),
    )).toBe("COMPLETED");
  });

  it("does not derive over a persisted business status", () => {
    expect(deriveBookingDisplayStatus(
      { ...confirmedBooking, status: "CANCELLED" },
      new Date("2026-07-05T10:30:00"),
    )).toBe("CANCELLED");
  });

  it("falls back safely for an unknown backend status", () => {
    expect(getBookingStatus("NEW_STATUS")).toEqual({
      label: "Không xác định",
      className: "bg-slate-100 text-slate-600",
    });
  });
});
