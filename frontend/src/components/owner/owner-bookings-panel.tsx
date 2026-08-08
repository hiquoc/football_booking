"use client";

import { useSearchParams } from "next/navigation";
import { BookingListContent } from "@/components/bookings/booking-list-content";

export function OwnerBookingsPanel() {
  const searchParams = useSearchParams();
  const page = Math.max(0, (Number(searchParams.get("page")) || 1) - 1);
  const today = new Date().toISOString().split("T")[0];
  const bookingDate = searchParams.get("bookingDate")?.trim();
  const bookingDateCleared = searchParams.get("bookingDateCleared") === "1";
  const filters = {
    bookingDate: bookingDate || (searchParams.has("bookingDate") || bookingDateCleared ? undefined : today),
    fieldId: searchParams.get("fieldId")?.trim() || undefined,
    fieldType: searchParams.get("fieldType")?.trim() || undefined,
    subFieldType: searchParams.get("subFieldType")?.trim() || undefined,
    status: searchParams.get("status")?.trim() || "COMPLETED",
  };

  return <BookingListContent page={page} owner filters={filters} bookingDateCleared={bookingDateCleared} />;
}
