import type { Notification, PageResponse } from "@/lib/api/types";
import { requestJson } from "./http";

export function fetchNotifications(page: number, size = 20) {
  return requestJson<PageResponse<Notification>>(
    `/api/notifications?page=${page}&size=${size}`,
  );
}

export function fetchUnreadCount() {
  return requestJson<{ count: number }>("/api/notifications/unread-count");
}

export function submitNotificationRead(id: string) {
  return requestJson<Notification>(
    `/api/notifications/${encodeURIComponent(id)}/read`,
    { method: "PATCH" },
  );
}

export function submitAllNotificationsRead() {
  return requestJson<void>("/api/notifications/read-all", { method: "PATCH" });
}

export function fetchNotificationSocketTicket() {
  return requestJson<{ ticket: string; expiresAt: string }>(
    "/api/notifications/socket-ticket",
    { method: "POST" },
  );
}
