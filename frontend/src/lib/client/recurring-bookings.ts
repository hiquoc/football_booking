import type {
  PageResponse,
  RecurringBooking,
  RecurringBookingInput,
  RecurringBookingStatus,
} from "@/lib/api/types";
import { jsonBody, requestJson } from "./http";

function listUrl(scope: "my" | "owner" | "admin", page: number, size: number, status?: RecurringBookingStatus) {
  const query = new URLSearchParams({ page: String(page), size: String(size) });
  if (status) query.set("status", status);
  return `/api/recurring-bookings/${scope}?${query}`;
}

export function fetchRecurringBookings(scope: "my" | "owner" | "admin", page: number, size = 10, status?: RecurringBookingStatus) {
  return requestJson<PageResponse<RecurringBooking>>(listUrl(scope, page, size, status));
}

export function submitRecurringBooking(input: RecurringBookingInput) {
  return requestJson<RecurringBooking>("/api/recurring-bookings", {
    method: "POST",
    ...jsonBody(input),
  });
}

export function submitRecurringBookingUpdate(id: string, input: RecurringBookingInput) {
  return requestJson<RecurringBooking>(`/api/recurring-bookings/${encodeURIComponent(id)}`, {
    method: "PUT",
    ...jsonBody(input),
  });
}

export function submitRecurringBookingAction(
  id: string,
  action: "pause" | "resume" | "cancel",
  scope: "my" | "owner" | "admin" = "my",
) {
  const query = scope === "admin" ? "?admin=true" : scope === "owner" ? "?owner=true" : "";
  return requestJson<RecurringBooking>(`/api/recurring-bookings/${encodeURIComponent(id)}/${action}${query}`, {
    method: action === "cancel" ? "DELETE" : "PATCH",
  });
}
