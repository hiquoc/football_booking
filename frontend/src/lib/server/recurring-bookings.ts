import "server-only";

import type {
  PageResponse,
  RecurringBooking,
  RecurringBookingInput,
  RecurringBookingStatus,
} from "@/lib/api/types";
import { authenticatedGatewayRequest } from "./authenticated-gateway";

function listPath(scope: "my" | "owner" | "admin", page: number, size: number, status?: RecurringBookingStatus) {
  const query = new URLSearchParams({
    page: String(page),
    size: String(size),
    sort: "createdAt,desc",
  });
  if (status) query.set("status", status);
  return `/api/v1/recurring-bookings/${scope}?${query}`;
}

export function getMyRecurringBookings(page = 0, size = 10, status?: RecurringBookingStatus) {
  return authenticatedGatewayRequest<PageResponse<RecurringBooking>>(listPath("my", page, size, status));
}

export function getOwnerRecurringBookings(page = 0, size = 10, status?: RecurringBookingStatus) {
  return authenticatedGatewayRequest<PageResponse<RecurringBooking>>(listPath("owner", page, size, status));
}

export function getAdminRecurringBookings(page = 0, size = 10, status?: RecurringBookingStatus) {
  return authenticatedGatewayRequest<PageResponse<RecurringBooking>>(listPath("admin", page, size, status));
}

export function createRecurringBooking(input: RecurringBookingInput) {
  return authenticatedGatewayRequest<RecurringBooking>("/api/v1/recurring-bookings", {
    method: "POST",
    body: JSON.stringify(input),
  });
}

export function updateRecurringBooking(id: string, input: RecurringBookingInput) {
  return authenticatedGatewayRequest<RecurringBooking>(`/api/v1/recurring-bookings/${encodeURIComponent(id)}`, {
    method: "PUT",
    body: JSON.stringify(input),
  });
}

export function changeRecurringBookingStatus(id: string, action: "pause" | "resume" | "cancel", admin = false) {
  const path = admin
    ? `/api/v1/recurring-bookings/admin/${encodeURIComponent(id)}${action === "cancel" ? "" : `/${action}`}`
    : `/api/v1/recurring-bookings/${encodeURIComponent(id)}${action === "cancel" ? "" : `/${action}`}`;
  return authenticatedGatewayRequest<RecurringBooking>(path, {
    method: action === "cancel" ? "DELETE" : "PATCH",
  });
}
