"use client";

import { useSearchParams } from "next/navigation";
import { BookingListContent } from "@/components/bookings/booking-list-content";

export function OwnerBookingsPanel() {
  const searchParams = useSearchParams();
  const page = Math.max(0, (Number(searchParams.get("page")) || 1) - 1);

  return <BookingListContent page={page} owner />;
}
