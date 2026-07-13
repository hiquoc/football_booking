"use client";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import type { CreateCheckoutInput } from "@/lib/api/types";
import { createCheckout, fetchPayment } from "@/lib/client/payments";
import { bookingQueryKeys, paymentQueryKeys } from "@/lib/query-keys";
export function usePayment(bookingId: string, enabled = true) {
  return useQuery({
    queryKey: paymentQueryKeys.byBooking(bookingId), queryFn: () => fetchPayment(bookingId), enabled,
    refetchInterval: (query) => query.state.data?.status === "PENDING" ? 2000 : false,
    retry: (count, error) => count < 3 && !error.message.toLowerCase().includes("not found"),
  });
}
export function useCreateCheckout() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (input: CreateCheckoutInput) => createCheckout(input),
    retry: 3,
    retryDelay: 3000,
    onSuccess: (_data, input) => {
      void queryClient.invalidateQueries({ queryKey: paymentQueryKeys.byBooking(input.bookingId) });
      void queryClient.invalidateQueries({ queryKey: bookingQueryKeys.detail(input.bookingId) });
    },
  });
}
