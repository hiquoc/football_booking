"use client";

import { useEffect } from "react";
import { Client } from "@stomp/stompjs";
import { useQueryClient } from "@tanstack/react-query";
import type { Notification, PageResponse, User, UserBalanceUpdateMessage } from "@/lib/api/types";
import { fetchNotificationSocketTicket } from "@/lib/client/notifications";
import { notificationQueryKeys, userQueryKeys } from "@/lib/query-keys";

const gatewaySocketUrl = process.env.NEXT_PUBLIC_GATEWAY_WS_URL ?? "ws://localhost:8080/ws";

function isNotificationList(data: unknown): data is PageResponse<Notification> {
  return typeof data === "object"
    && data !== null
    && Array.isArray((data as PageResponse<Notification>).content);
}

function isNotificationListQueryKey(queryKey: readonly unknown[]) {
  return queryKey[0] === notificationQueryKeys.all[0] && queryKey[1] === "list";
}

function isUserList(data: unknown): data is PageResponse<User> {
  return typeof data === "object"
    && data !== null
    && Array.isArray((data as PageResponse<User>).content);
}

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
              const cachedNotificationLists = queryClient.getQueriesData<PageResponse<Notification>>({
                queryKey: notificationQueryKeys.all,
                predicate: (query) => isNotificationListQueryKey(query.queryKey),
              });
              const isCachedNotification = cachedNotificationLists.some(([, data]) =>
                isNotificationList(data) && data.content.some((item) => item.id === notification.id),
              );

              queryClient.setQueriesData<PageResponse<Notification>>(
                {
                  queryKey: notificationQueryKeys.all,
                  predicate: (query) => isNotificationListQueryKey(query.queryKey),
                },
                (old) => {
                  if (!isNotificationList(old) || old.page !== 0) return old;

                  const alreadyInList = old.content.some((item) => item.id === notification.id);
                  return {
                    ...old,
                    content: [
                      notification,
                      ...old.content.filter((item) => item.id !== notification.id),
                    ].slice(0, old.size),
                    totalElements: old.totalElements + (alreadyInList || isCachedNotification ? 0 : 1),
                    empty: false,
                  };
                },
              );
              queryClient.setQueryData<{ count: number }>(
                notificationQueryKeys.unreadCount,
                (old) => ({ count: (old?.count ?? 0) + (!notification.isRead && !isCachedNotification ? 1 : 0) }),
              );
            });
            client?.subscribe("/user/queue/balance", (message) => {
              const balanceUpdate = JSON.parse(message.body) as UserBalanceUpdateMessage;
              queryClient.setQueryData<User>(userQueryKeys.mePrivate, (old) =>
                old ? { ...old, balance: balanceUpdate.balance } : old,
              );
              queryClient.setQueriesData<PageResponse<User>>(
                { queryKey: userQueryKeys.all },
                (old) => isUserList(old)
                  ? {
                    ...old,
                    content: old.content.map((user) =>
                      user.id === balanceUpdate.userId
                        ? { ...user, balance: balanceUpdate.balance }
                        : user,
                    ),
                  }
                  : old,
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
