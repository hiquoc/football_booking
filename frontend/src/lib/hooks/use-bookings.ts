"use client";

import {
  useMutation,
  useQueries,
  useQuery,
  useQueryClient,
} from "@tanstack/react-query";
import type { Booking, CreateBookingInput, PageResponse } from "@/lib/api/types";
import {
  fetchAvailability,
  fetchBooking,
  fetchMyBookings,
  fetchOwnerBookings,
  submitBooking,
  submitCancellation,
  submitMockPayment,
} from "@/lib/client/bookings";
import { bookingQueryKeys } from "@/lib/query-keys";

export function useMyBookings(page: number, size = 10) {
  return useQuery({
    queryKey: bookingQueryKeys.mine(page, size),
    queryFn: () => fetchMyBookings(page, size),
  });
}

export function useOwnerBookings(page: number, size = 10) {
  return useQuery({
    queryKey: bookingQueryKeys.owner(page, size),
    queryFn: () => fetchOwnerBookings(page, size),
  });
}

export function useBookingList(page: number, owner = false, size = 10) {
  return useQuery({
    queryKey: owner
      ? bookingQueryKeys.owner(page, size)
      : bookingQueryKeys.mine(page, size),
    queryFn: () =>
      owner ? fetchOwnerBookings(page, size) : fetchMyBookings(page, size),
  });
}

export function useBooking(id: string) {
  return useQuery({
    queryKey: bookingQueryKeys.detail(id),
    queryFn: () => fetchBooking(id),
  });
}

export function useAvailability(subFieldId: string, date: string) {
  return useQuery({
    queryKey: bookingQueryKeys.availability(subFieldId, date),
    queryFn: () => fetchAvailability(subFieldId, date),
    enabled: Boolean(subFieldId && date),
  });
}

export function useSubFieldAvailabilities(subFieldIds: string[], date: string) {
  return useQueries({
    queries: subFieldIds.map((subFieldId) => ({
      queryKey: bookingQueryKeys.availability(subFieldId, date),
      queryFn: () => fetchAvailability(subFieldId, date),
      enabled: Boolean(subFieldId && date),
    })),
  });
}

export function useCreateBooking() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (input: CreateBookingInput) => submitBooking(input),
    retry: false,
    onSuccess: (booking, input) => {
      queryClient.setQueryData(bookingQueryKeys.detail(booking.id), booking);
      void queryClient.invalidateQueries({ queryKey: bookingQueryKeys.all });
      void queryClient.invalidateQueries({
        queryKey: bookingQueryKeys.availability(
          input.subFieldId,
          input.bookingDate,
        ),
      });
    },
  });
}

export function useCancelBooking(owner = false) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, reason }: { id: string; reason?: string }) =>
      submitCancellation(id, reason, owner),
    retry: false,
    onMutate: async ({ id, reason }) => {
      await queryClient.cancelQueries({ queryKey: bookingQueryKeys.all });
      const snapshot = queryClient.getQueriesData({ queryKey: bookingQueryKeys.all });
      const update = (booking: Booking) => booking.id === id ? { ...booking, status: "CANCELLED" as const, cancellationReason: reason ?? null } : booking;
      queryClient.setQueryData<Booking>(bookingQueryKeys.detail(id), (old) => old ? update(old) : old);
      queryClient.setQueriesData<PageResponse<Booking>>({ queryKey: bookingQueryKeys.all }, (old) => old ? { ...old, content: old.content.map(update) } : old);
      return snapshot;
    },
    onError: (_error, _variables, snapshot) => snapshot?.forEach(([key, data]) => queryClient.setQueryData(key, data)),
    onSuccess: (booking) => {
      queryClient.setQueryData(bookingQueryKeys.detail(booking.id), booking);
      void queryClient.invalidateQueries({ queryKey: bookingQueryKeys.all });
    },
  });
}

export function useMockPayment() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: submitMockPayment,
    retry: false,
    onSuccess: (booking) => {
      queryClient.setQueryData(bookingQueryKeys.detail(booking.id), booking);
      void queryClient.invalidateQueries({ queryKey: bookingQueryKeys.all });
    },
  });
}
