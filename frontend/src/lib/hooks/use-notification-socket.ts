"use client";

import { useEffect } from "react";
import { Client } from "@stomp/stompjs";
import { useQueryClient } from "@tanstack/react-query";
import type { Notification, PageResponse } from "@/lib/api/types";
import { fetchNotificationSocketTicket } from "@/lib/client/notifications";
import { notificationQueryKeys } from "@/lib/query-keys";

const gatewaySocketUrl = process.env.NEXT_PUBLIC_GATEWAY_WS_URL ?? "ws://localhost:8080/ws";

export function useNotificationSocket(enabled = true) {
  const queryClient = useQueryClient();

  useEffect(() => {
    if (!enabled) return;
    let disposed = false;
    let client: Client | null = null;
    let reconnectTimer: ReturnType<typeof setTimeout> | null = null;

    const connect = async () => {
      try {
        const { ticket } = await fetchNotificationSocketTicket();
        if (disposed) return;
        client = new Client({
        brokerURL: `${gatewaySocketUrl}?ticket=${encodeURIComponent(ticket)}`,
        reconnectDelay: 0,
        heartbeatIncoming: 10_000,
        heartbeatOutgoing: 10_000,
        debug: () => undefined,
        onConnect: () => {
          client?.subscribe("/user/queue/notifications", (message) => {
            const notification = JSON.parse(message.body) as Notification;
            queryClient.setQueryData<PageResponse<Notification>>(
              notificationQueryKeys.list(0, 20),
              (old) => old ? {
                ...old,
                content: [notification, ...old.content.filter((item) => item.id !== notification.id)].slice(0, old.size),
                totalElements: old.totalElements + (old.content.some((item) => item.id === notification.id) ? 0 : 1),
                empty: false,
              } : old,
            );
            queryClient.setQueryData<{ count: number }>(
              notificationQueryKeys.unreadCount,
              (old) => ({ count: (old?.count ?? 0) + 1 }),
            );
          });
        },
        onWebSocketClose: () => {
          if (!disposed) reconnectTimer = setTimeout(() => void connect(), 5_000);
        },
      });
        client.activate();
      } catch {
        if (!disposed) reconnectTimer = setTimeout(() => void connect(), 15_000);
      }
    };
    void connect();

    return () => {
      disposed = true;
      if (reconnectTimer) clearTimeout(reconnectTimer);
      void client?.deactivate();
    };
  }, [enabled, queryClient]);
}
