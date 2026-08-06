"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import type {
  Booking,
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
import { userQueryKeys } from "@/lib/query-keys";

function decrementCurrentUserBalance(
  queryClient: ReturnType<typeof useQueryClient>,
  booking: Booking | null | undefined,
) {
  if (booking?.paymentStatus !== "PAID") return;
  const amount = Number(booking.bookingPrice ?? booking.platformBookingFee ?? 0);
  if (!Number.isFinite(amount) || amount <= 0) return;
  queryClient.setQueryData(userQueryKeys.mePrivate, (old: unknown) => {
    if (!old || typeof old !== "object") return old;
    const user = old as { balance?: number };
    if (typeof user.balance !== "number") return old;
    return { ...user, balance: Math.max(0, user.balance - amount) };
  });
}

function updateRecurringBookingInCache(
  queryClient: ReturnType<typeof useQueryClient>,
  recurringBooking: RecurringBooking,
) {
  queryClient.setQueriesData<PageResponse<RecurringBooking>>(
    { queryKey: recurringBookingQueryKeys.all },
    (old) =>
      old
        ? {
            ...old,
            content: old.content.map((item) =>
              item.id === recurringBooking.id ? recurringBooking : item,
            ),
          }
        : old,
  );
}

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
    onSuccess: (recurringBooking) => {
      decrementCurrentUserBalance(queryClient, recurringBooking.firstBooking);
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
    onSuccess: (recurringBooking) => {
      updateRecurringBookingInCache(queryClient, recurringBooking);
    },
  });
}

export function useRecurringBookingAction(scope: "my" | "owner" | "admin" = "my") {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, action }: { id: string; action: "pause" | "resume" | "cancel" }) =>
      submitRecurringBookingAction(id, action, scope),
    retry: false,
    onSuccess: (recurringBooking) => {
      updateRecurringBookingInCache(queryClient, recurringBooking);
      if (recurringBooking.latestBooking) {
        queryClient.setQueryData(
          bookingQueryKeys.detail(recurringBooking.latestBooking.id),
          recurringBooking.latestBooking,
        );
      }
    },
  });
}
