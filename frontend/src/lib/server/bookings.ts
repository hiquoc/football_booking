import "server-only";

import type {
  Availability,
  Booking,
  CreateBookingInput,
  PageResponse,
} from "@/lib/api/types";
import { authenticatedGatewayRequest } from "./authenticated-gateway";
import { gatewayRequest } from "./gateway";

export function getMyBookings(page = 0, size = 10) {
  return authenticatedGatewayRequest<PageResponse<Booking>>(
    `/api/v1/bookings/my?page=${page}&size=${size}&sort=createdAt,desc`,
  );
}

export function getOwnerBookings(page = 0, size = 10) {
  return authenticatedGatewayRequest<PageResponse<Booking>>(
    `/api/v1/bookings/owner?page=${page}&size=${size}&sort=createdAt,desc`,
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

export function confirmMockPayment(id: string) {
  return authenticatedGatewayRequest<Booking>(
    `/api/v1/bookings/${encodeURIComponent(id)}/mock-payment`,
    { method: "PATCH" },
  );
}
