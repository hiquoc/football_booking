"use client";

import { useSearchParams } from "next/navigation";
import { BookingListContent } from "@/components/bookings/booking-list-content";

export function OwnerBookingsPanel() {
  const searchParams = useSearchParams();
  const page = Math.max(0, (Number(searchParams.get("page")) || 1) - 1);
  const filters = {
    bookingDate: searchParams.get("bookingDate") ?? undefined,
    subFieldId: searchParams.get("subFieldId") ?? undefined,
    status: searchParams.get("status") ?? undefined,
  };

  return <BookingListContent page={page} owner filters={filters} />;
}
