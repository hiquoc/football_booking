import type {
  Availability,
  Booking,
  CreateBookingInput,
  PageResponse,
} from "@/lib/api/types";
import { jsonBody, requestJson } from "./http";

export function fetchMyBookings(page: number, size = 10) {
  return requestJson<PageResponse<Booking>>(
    `/api/bookings?page=${page}&size=${size}`,
  );
}

export function fetchOwnerBookings(page: number, size = 10) {
  return requestJson<PageResponse<Booking>>(
    `/api/owner/bookings?page=${page}&size=${size}`,
  );
}

export function fetchBooking(id: string) {
  return requestJson<Booking>(`/api/bookings/${encodeURIComponent(id)}`);
}

export function fetchAvailability(subFieldId: string, date: string) {
  const query = new URLSearchParams({ subFieldId, date });
  return requestJson<Availability>(`/api/bookings/availability?${query}`);
}

export function submitBooking(input: CreateBookingInput) {
  return requestJson<Booking>("/api/bookings", {
    method: "POST",
    ...jsonBody(input),
  });
}

export function submitCancellation(id: string, reason?: string, owner = false) {
  return requestJson<Booking>(
    `/api/${owner ? "owner/" : ""}bookings/${encodeURIComponent(id)}/cancel`,
    { method: "PATCH", ...jsonBody({ reason }) },
  );
}

export function submitMockPayment(id: string) {
  return requestJson<Booking>(
    `/api/bookings/${encodeURIComponent(id)}/payment`,
    { method: "PATCH" },
  );
}
