import "server-only";

import type {
  Availability,
  Booking,
  BookingConfig,
  CreateBookingInput,
  MatchResultInput,
  PageResponse,
} from "@/lib/api/types";
import { authenticatedGatewayRequest } from "./authenticated-gateway";
import { gatewayRequest } from "./gateway";

function todayInVietnam() {
  return new Date().toLocaleDateString("en-CA", {
    timeZone: "Asia/Ho_Chi_Minh",
  });
}

export function getMyBookings(
  page = 0,
  size = 10,
  filters: { bookingDate?: string; status?: string } = {},
) {
  const query = new URLSearchParams({
    page: String(page),
    size: String(size),
    sort: "createdAt,desc",
  });
  Object.entries(filters).forEach(([key, value]) => {
    if (value) query.set(key, value);
  });
  return authenticatedGatewayRequest<PageResponse<Booking>>(
    `/api/v1/bookings/my?${query}`,
  );
}

export function getOwnerBookings(
  page = 0,
  size = 10,
  filters: { bookingDate?: string; subFieldId?: string; status?: string } = {},
) {
  const query = new URLSearchParams({
    page: String(page),
    size: String(size),
    sort: "startDateTime,asc",
  });
  query.set("bookingDate", filters.bookingDate ?? todayInVietnam());
  query.set("status", filters.status ?? "COMPLETED");
  Object.entries(filters).forEach(([key, value]) => {
    if (value) query.set(key, value);
  });
  return authenticatedGatewayRequest<PageResponse<Booking>>(
    `/api/v1/bookings/owner?${query}`,
  );
}

export function getBooking(id: string) {
  return authenticatedGatewayRequest<Booking>(
    `/api/v1/bookings/${encodeURIComponent(id)}`,
  );
}

export function getAvailability(subFieldId: string, date: string) {
  const query = new URLSearchParams({ subFieldId, date });
  return gatewayRequest<Availability>(
    `/api/v1/bookings/availability?${query}`,
    { cache: "no-store" },
  );
}

export function getBookingConfig() {
  return gatewayRequest<BookingConfig>("/api/v1/bookings/config", {
    next: { revalidate: 31_536_000 },
  });
}

export function createBooking(input: CreateBookingInput) {
  return authenticatedGatewayRequest<Booking>("/api/v1/bookings", {
    method: "POST",
    body: JSON.stringify(input),
  });
}

export function cancelBooking(
  bookingId: string,
  reason?: string,
  owner = false,
) {
  return authenticatedGatewayRequest<Booking>(
    owner ? "/api/v1/bookings/owner/cancel" : "/api/v1/bookings/cancel",
    {
      method: "PATCH",
      body: JSON.stringify({ bookingId, reason }),
    },
  );
}

export function upsertMatchResult(bookingId: string, input: MatchResultInput) {
  return authenticatedGatewayRequest<Booking>(
    `/api/v1/bookings/owner/${encodeURIComponent(bookingId)}/match-result`,
    {
      method: "PUT",
      body: JSON.stringify(input),
    },
  );
}
