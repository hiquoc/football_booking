"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  fetchNotifications,
  fetchUnreadCount,
  submitAllNotificationsRead,
  submitNotificationRead,
} from "@/lib/client/notifications";
import { notificationQueryKeys } from "@/lib/query-keys";
import type { Notification, PageResponse } from "@/lib/api/types";

type NotificationSnapshot = Array<[readonly unknown[], unknown]>;

function markCachedNotifications(
  queryClient: ReturnType<typeof useQueryClient>,
  predicate: (notification: Notification) => boolean,
) {
  queryClient.setQueriesData<PageResponse<Notification>>(
    { queryKey: notificationQueryKeys.all },
    (old) => old ? { ...old, content: old.content?.map((item) => predicate(item) ? { ...item, isRead: true, readAt: new Date().toISOString() } : item) } : old,
  );
}

export function useNotifications(page: number, size = 20) {
  return useQuery({
    queryKey: notificationQueryKeys.list(page, size),
    queryFn: () => fetchNotifications(page, size),
  });
}

export function useUnreadNotificationCount(enabled = true) {
  return useQuery({
    queryKey: notificationQueryKeys.unreadCount,
    queryFn: fetchUnreadCount,
    enabled,
    refetchInterval: 60_000,
  });
}

export function useMarkNotificationRead() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: submitNotificationRead,
    onMutate: async (id) => {
      await queryClient.cancelQueries({ queryKey: notificationQueryKeys.all });
      const snapshot = queryClient.getQueriesData({ queryKey: notificationQueryKeys.all }) as NotificationSnapshot;
      markCachedNotifications(queryClient, (item) => item.id === id);
      queryClient.setQueryData<{ count: number }>(notificationQueryKeys.unreadCount, (old) => old ? { count: Math.max(0, old.count - 1) } : old);
      return snapshot;
    },
    onError: (_error, _id, snapshot) =>{console.log(_error); snapshot?.forEach(([key, data]) => queryClient.setQueryData(key, data))},
    onSuccess: () => void queryClient.invalidateQueries({ queryKey: notificationQueryKeys.all }),
  });
}

export function useMarkAllNotificationsRead() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: submitAllNotificationsRead,
    onMutate: async () => {
      await queryClient.cancelQueries({ queryKey: notificationQueryKeys.all });
      const snapshot = queryClient.getQueriesData({ queryKey: notificationQueryKeys.all }) as NotificationSnapshot;
      markCachedNotifications(queryClient, () => true);
      queryClient.setQueryData(notificationQueryKeys.unreadCount, { count: 0 });
      return snapshot;
    },
    onError: (_error, _variables, snapshot) => snapshot?.forEach(([key, data]) => queryClient.setQueryData(key, data)),
    onSuccess: () => void queryClient.invalidateQueries({ queryKey: notificationQueryKeys.all }),
  });
}
