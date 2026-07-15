"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import type {
  PageResponse,
  RecurringBooking,
  RecurringBookingInput,
  RecurringBookingStatus,
} from "@/lib/api/types";
import {
  fetchRecurringBookings,
  submitRecurringBooking,
  submitRecurringBookingAction,
  submitRecurringBookingUpdate,
} from "@/lib/client/recurring-bookings";
import { bookingQueryKeys, recurringBookingQueryKeys } from "@/lib/query-keys";

export function useRecurringBookings(scope: "my" | "owner" | "admin", page: number, size = 10, status?: RecurringBookingStatus) {
  return useQuery({
    queryKey: recurringBookingQueryKeys[scope === "my" ? "mine" : scope](page, size, status),
    queryFn: () => fetchRecurringBookings(scope, page, size, status),
  });
}

export function useCreateRecurringBooking() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (input: RecurringBookingInput) => submitRecurringBooking(input),
    retry: false,
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: recurringBookingQueryKeys.all });
      void queryClient.invalidateQueries({ queryKey: bookingQueryKeys.all });
    },
  });
}

export function useUpdateRecurringBooking() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, input }: { id: string; input: RecurringBookingInput }) =>
      submitRecurringBookingUpdate(id, input),
    retry: false,
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: recurringBookingQueryKeys.all });
      void queryClient.invalidateQueries({ queryKey: bookingQueryKeys.all });
    },
  });
}

export function useRecurringBookingAction(admin = false) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, action }: { id: string; action: "pause" | "resume" | "cancel" }) =>
      submitRecurringBookingAction(id, action, admin),
    retry: false,
    onMutate: async ({ id, action }) => {
      await queryClient.cancelQueries({ queryKey: recurringBookingQueryKeys.all });
      const snapshot = queryClient.getQueriesData({ queryKey: recurringBookingQueryKeys.all });
      const nextStatus = action === "pause" ? "PAUSED" : action === "resume" ? "ACTIVE" : "CANCELLED";
      const update = (item: RecurringBooking) => item.id === id ? { ...item, status: nextStatus as RecurringBookingStatus } : item;
      queryClient.setQueriesData<PageResponse<RecurringBooking>>({ queryKey: recurringBookingQueryKeys.all }, (old) =>
        old ? { ...old, content: old.content.map(update) } : old,
      );
      return snapshot;
    },
    onError: (_error, _variables, snapshot) => snapshot?.forEach(([key, data]) => queryClient.setQueryData(key, data)),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: recurringBookingQueryKeys.all });
      void queryClient.invalidateQueries({ queryKey: bookingQueryKeys.all });
    },
  });
}
