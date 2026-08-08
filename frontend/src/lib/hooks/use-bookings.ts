"use client";

import {
  useMutation,
  useQueries,
  useQuery,
  useQueryClient,
} from "@tanstack/react-query";
import type {
  Booking,
  CreateBookingInput,
  CreatePaymentDisputeInput,
  MatchResultInput,
  PageResponse,
  RecurringBooking,
} from "@/lib/api/types";
import {
  fetchAvailability,
  fetchBooking,
  fetchBookingConfig,
  fetchMyBookings,
  fetchOwnerBookings,
  fetchOwnerReservations,
  submitBooking,
  submitBookingPayment,
  submitCancellation,
  submitMatchResult,
  submitReservation,
  submitReservationCancellation,
} from "@/lib/client/bookings";
import { submitNoShowReport, submitPaymentDispute } from "@/lib/client/moderation";
import { bookingQueryKeys } from "@/lib/query-keys";
import { recurringBookingQueryKeys, userQueryKeys } from "@/lib/query-keys";

type BookingListFilters = {
  bookingDate?: string;
  fieldId?: string;
  fieldType?: string;
  subFieldId?: string;
  subFieldType?: string;
  status?: string;
};

function bookingPaymentAmount(booking: Booking) {
  return Number(booking.bookingPrice ?? booking.platformBookingFee ?? 0);
}

function decrementCurrentUserBalance(
  queryClient: ReturnType<typeof useQueryClient>,
  amount: number,
) {
  if (!Number.isFinite(amount) || amount <= 0) return;
  queryClient.setQueryData(userQueryKeys.mePrivate, (old: unknown) => {
    if (!old || typeof old !== "object") return old;
    const user = old as { balance?: number };
    if (typeof user.balance !== "number") return old;
    return { ...user, balance: Math.max(0, user.balance - amount) };
  });
}

function decrementBalanceForPaidBooking(
  queryClient: ReturnType<typeof useQueryClient>,
  booking: Booking,
) {
  if (booking.paymentStatus !== "PAID") return;
  decrementCurrentUserBalance(queryClient, bookingPaymentAmount(booking));
}

function findCachedBooking(
  queryClient: ReturnType<typeof useQueryClient>,
  id: string,
) {
  const detail = queryClient.getQueryData<Booking>(bookingQueryKeys.detail(id));
  if (detail) return detail;

  for (const [, page] of queryClient.getQueriesData<PageResponse<Booking>>({
    queryKey: bookingQueryKeys.all,
  })) {
    const booking = page?.content?.find((item) => item.id === id);
    if (booking) return booking;
  }
  return null;
}

function incrementCompletedBookingCount(queryClient: ReturnType<typeof useQueryClient>) {
  queryClient.setQueryData(userQueryKeys.mePrivate, (old: unknown) => {
    if (!old || typeof old !== "object") return old;
    const user = old as { completedBookingCount?: number };
    return {
      ...user,
      completedBookingCount: (user.completedBookingCount ?? 0) + 1,
    };
  });
}

export function useBookingConfig() {
  return useQuery({
    queryKey: bookingQueryKeys.config,
    queryFn: fetchBookingConfig,
    staleTime: 31_536_000_000,
    gcTime: 31_536_000_000,
  });
}

export function useMyBookings(
  page: number,
  size = 10,
  filters: { bookingDate?: string; status?: string } = {},
) {
  return useQuery({
    queryKey: bookingQueryKeys.mine(page, size, filters),
    queryFn: () => fetchMyBookings(page, size, filters),
  });
}

function markBookingReported(queryClient: ReturnType<typeof useQueryClient>, bookingId: string) {
  const update = (booking: Booking) =>
    booking.id === bookingId ? { ...booking, status: "REPORTED" as const } : booking;

  queryClient.setQueryData<Booking>(bookingQueryKeys.detail(bookingId), (old) =>
    old ? update(old) : old,
  );
  queryClient.setQueriesData<PageResponse<Booking>>({ queryKey: bookingQueryKeys.all }, (old) =>
    old ? { ...old, content: old.content?.map(update) } : old,
  );
}

export function useOwnerBookings(
  page: number,
  size = 10,
  filters: BookingListFilters = {},
) {
  return useQuery({
    queryKey: bookingQueryKeys.owner(page, size, filters),
    queryFn: () => fetchOwnerBookings(page, size, filters),
  });
}

export function useOwnerReservations(
  page: number,
  size = 10,
  filters: { bookingDate?: string; subFieldId?: string; status?: string } = {},
) {
  return useQuery({
    queryKey: bookingQueryKeys.reservations(page, size, filters),
    queryFn: () => fetchOwnerReservations(page, size, filters),
  });
}

export function useBookingList(
  page: number,
  owner = false,
  size = 10,
  filters: BookingListFilters = {},
  reservations = false,
) {
  return useQuery({
    queryKey: reservations
      ? bookingQueryKeys.reservations(page, size, filters)
      : owner
      ? bookingQueryKeys.owner(page, size, filters)
      : bookingQueryKeys.mine(page, size, filters),
    queryFn: () =>
      reservations
        ? fetchOwnerReservations(page, size, filters)
        : owner
          ? fetchOwnerBookings(page, size, filters)
          : fetchMyBookings(page, size, filters),
  });
}

export function useBooking(id: string) {
  const queryClient = useQueryClient();
  return useQuery({
    queryKey: bookingQueryKeys.detail(id),
    queryFn: () => fetchBooking(id),
    refetchInterval: (query) => query.state.data?.status === "PENDING" ? 1000 : false,
    select: (booking) => {
      const previous = queryClient.getQueryData<Booking>(bookingQueryKeys.detail(id));
      if (booking.status === "COMPLETED" && previous?.status && previous.status !== "COMPLETED") {
        incrementCompletedBookingCount(queryClient);
      }
      return booking;
    },
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
      decrementBalanceForPaidBooking(queryClient, booking);
      void queryClient.invalidateQueries({ queryKey: bookingQueryKeys.all });
      void queryClient.invalidateQueries({
        queryKey: bookingQueryKeys.availability(
          input.subFieldId,
          input.bookingDate ?? input.startDateTime.slice(0, 10),
        ),
      });
    },
  });
}

export function useCreateReservation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (input: CreateBookingInput) => submitReservation(input),
    retry: false,
    onSuccess: (booking, input) => {
      queryClient.setQueryData(bookingQueryKeys.detail(booking.id), booking);
      void queryClient.invalidateQueries({ queryKey: bookingQueryKeys.all });
      void queryClient.invalidateQueries({
        queryKey: bookingQueryKeys.availability(
          input.subFieldId,
          input.bookingDate ?? input.startDateTime.slice(0, 10),
        ),
      });
    },
  });
}

export function useCancelReservation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, reason }: { id: string; reason?: string }) =>
      submitReservationCancellation(id, reason),
    retry: false,
    onSuccess: (booking) => {
      queryClient.setQueryData(bookingQueryKeys.detail(booking.id), booking);
      void queryClient.invalidateQueries({ queryKey: bookingQueryKeys.all });
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
      queryClient.setQueriesData<PageResponse<Booking>>({ queryKey: bookingQueryKeys.all }, (old) => old ? { ...old, content: old.content?.map(update) } : old);
      return snapshot;
    },
    onError: (_error, _variables, snapshot) => snapshot?.forEach(([key, data]) => queryClient.setQueryData(key, data)),
    onSuccess: (booking) => {
      queryClient.setQueryData(bookingQueryKeys.detail(booking.id), booking);
      queryClient.setQueriesData<PageResponse<RecurringBooking>>(
        { queryKey: recurringBookingQueryKeys.all },
        (old) =>
          old
            ? {
                ...old,
                content: old.content.map((item) =>
                  item.latestBooking?.id === booking.id
                    ? { ...item, latestBooking: booking }
                    : item,
                ),
              }
            : old,
      );
      void queryClient.invalidateQueries({ queryKey: bookingQueryKeys.all });
    },
  });
}

export function usePayBooking() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => submitBookingPayment(id),
    retry: false,
    onMutate: async (id) => {
      await queryClient.cancelQueries({ queryKey: userQueryKeys.mePrivate });
      const previousUser = queryClient.getQueryData(userQueryKeys.mePrivate);
      const booking = findCachedBooking(queryClient, id);
      const amount = booking ? bookingPaymentAmount(booking) : 0;
      decrementCurrentUserBalance(queryClient, amount);
      return { previousUser, deducted: amount > 0 };
    },
    onError: (_error, _id, context) => {
      queryClient.setQueryData(userQueryKeys.mePrivate, context?.previousUser);
    },
    onSuccess: (booking, _id, context) => {
      if (!context?.deducted) decrementBalanceForPaidBooking(queryClient, booking);
      queryClient.setQueryData(bookingQueryKeys.detail(booking.id), booking);
      void queryClient.invalidateQueries({ queryKey: bookingQueryKeys.all });
      void queryClient.invalidateQueries({ queryKey: recurringBookingQueryKeys.all });
    },
  });
}

export function useSubmitMatchResult() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ bookingId, input }: { bookingId: string; input: MatchResultInput }) =>
      submitMatchResult(bookingId, input),
    retry: false,
    onSuccess: (booking) => {
      queryClient.setQueryData(bookingQueryKeys.detail(booking.id), booking);
      void queryClient.invalidateQueries({ queryKey: bookingQueryKeys.all });
      void queryClient.invalidateQueries({ queryKey: userQueryKeys.all });
    },
  });
}

export function useReportNoShow() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (bookingId: string) => submitNoShowReport(bookingId),
    retry: false,
    onSuccess: (_response, bookingId) => {
      markBookingReported(queryClient, bookingId);
      void queryClient.invalidateQueries({ queryKey: bookingQueryKeys.all });
    },
  });
}

export function useCreatePaymentDispute() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (input: CreatePaymentDisputeInput) => submitPaymentDispute(input),
    retry: false,
    onSuccess: (_response, input) => {
      markBookingReported(queryClient, input.bookingId);
      void queryClient.invalidateQueries({ queryKey: bookingQueryKeys.all });
    },
  });
}
