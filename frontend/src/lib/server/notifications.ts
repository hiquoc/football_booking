import "server-only";

import type { Notification, PageResponse } from "@/lib/api/types";
import { authenticatedGatewayRequest } from "./authenticated-gateway";

export function getNotifications(page = 0, size = 20) {
  return authenticatedGatewayRequest<PageResponse<Notification>>(
    `/api/v1/notifications?page=${page}&size=${size}&sort=createdAt,desc`,
  );
}

export async function getUnreadCount() {
  return authenticatedGatewayRequest<{ count: number }>(
    "/api/v1/notifications/unread-count",
  );
}

export function markNotificationRead(id: string) {
  return authenticatedGatewayRequest<Notification>(
    `/api/v1/notifications/${encodeURIComponent(id)}/read`,
    { method: "PATCH" },
  );
}

export function markAllNotificationsRead() {
  return authenticatedGatewayRequest<null>("/api/v1/notifications/read-all", {
    method: "PATCH",
  });
}

export function createNotificationSocketTicket() {
  return authenticatedGatewayRequest<{ ticket: string; expiresAt: string }>(
    "/api/v1/ws-ticket",
    { method: "POST" },
  );
}
