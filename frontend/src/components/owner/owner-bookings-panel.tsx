"use client";

import { useSearchParams } from "next/navigation";
import { BookingListContent } from "@/components/bookings/booking-list-content";

function todayInVietnam() {
  return new Date().toLocaleDateString("en-CA", {
    timeZone: "Asia/Ho_Chi_Minh",
  });
}

export function OwnerBookingsPanel() {
  const searchParams = useSearchParams();
  const page = Math.max(0, (Number(searchParams.get("page")) || 1) - 1);
  const filters = {
    bookingDate: searchParams.get("bookingDate") ?? todayInVietnam(),
    subFieldId: searchParams.get("subFieldId") ?? undefined,
    status: searchParams.get("status") ?? "COMPLETED",
  };

  return <BookingListContent page={page} owner filters={filters} />;
}
