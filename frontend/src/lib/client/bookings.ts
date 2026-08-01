import type {
  Availability,
  Booking,
  BookingConfig,
  CreateBookingInput,
  MatchResultInput,
  PageResponse,
} from "@/lib/api/types";
import { jsonBody, requestJson } from "./http";

export function fetchMyBookings(
  page: number,
  size = 10,
  filters: { bookingDate?: string; status?: string } = {},
) {
  const query = new URLSearchParams({ page: String(page), size: String(size) });
  Object.entries(filters).forEach(([key, value]) => {
    if (value && value !== "ALL") query.set(key, value);
  });
  return requestJson<PageResponse<Booking>>(
    `/api/bookings?${query}`,
  );
}

export function fetchOwnerBookings(
  page: number,
  size = 10,
  filters: { bookingDate?: string; fieldId?: string; fieldType?: string; subFieldType?: string; status?: string } = {},
) {
  const query = new URLSearchParams({ page: String(page), size: String(size) });
  Object.entries(filters).forEach(([key, value]) => {
    if (value && value !== "ALL") query.set(key, value);
  });
  return requestJson<PageResponse<Booking>>(
    `/api/owner/bookings?${query}`,
  );
}

export function fetchOwnerReservations(
  page: number,
  size = 10,
  filters: { bookingDate?: string; subFieldId?: string; status?: string } = {},
) {
  const query = new URLSearchParams({ page: String(page), size: String(size) });
  Object.entries(filters).forEach(([key, value]) => {
    if (value) query.set(key, value);
  });
  return requestJson<PageResponse<Booking>>(
    `/api/owner/reservations?${query}`,
  );
}

export function fetchBooking(id: string) {
  return requestJson<Booking>(`/api/bookings/${encodeURIComponent(id)}`);
}

export function fetchAvailability(subFieldId: string, date: string) {
  const query = new URLSearchParams({ subFieldId, date });
  return requestJson<Availability>(`/api/bookings/availability?${query}`);
}

export function fetchBookingConfig() {
  return requestJson<BookingConfig>("/api/bookings/config", {
    cache: "force-cache",
  });
}

export function submitBooking(input: CreateBookingInput) {
  return requestJson<Booking>("/api/bookings", {
    method: "POST",
    ...jsonBody(input),
  });
}

export function submitReservation(input: CreateBookingInput) {
  return requestJson<Booking>("/api/owner/reservations", {
    method: "POST",
    ...jsonBody(input),
  });
}

export function submitReservationCancellation(id: string, reason?: string) {
  return requestJson<Booking>(
    `/api/owner/reservations/${encodeURIComponent(id)}/cancel`,
    { method: "PATCH", ...jsonBody({ reason }) },
  );
}

export function submitCancellation(id: string, reason?: string, owner = false) {
  return requestJson<Booking>(
    `/api/${owner ? "owner/" : ""}bookings/${encodeURIComponent(id)}/cancel`,
    { method: "PATCH", ...jsonBody({ reason }) },
  );
}

export function submitMatchResult(bookingId: string, input: MatchResultInput) {
  return requestJson<Booking>(
    `/api/owner/bookings/${encodeURIComponent(bookingId)}/match-result`,
    { method: "PUT", ...jsonBody(input) },
  );
}
