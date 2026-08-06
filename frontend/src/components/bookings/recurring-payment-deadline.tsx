"use client";

import { useEffect } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { CreditCard, LoaderCircle, TimerReset } from "lucide-react";
import type { Booking } from "@/lib/api/types";
import { openWalletTopUpPanel } from "@/lib/client/wallet-top-up-panel";
import { usePayBooking } from "@/lib/hooks/use-bookings";
import { useCountdown } from "@/lib/hooks/use-countdown";
import { useCurrentUser } from "@/lib/hooks/use-profile";
import { bookingQueryKeys, recurringBookingQueryKeys } from "@/lib/query-keys";

export function RecurringPaymentDeadline({
  booking,
  compact = false,
}: {
  booking: Booking;
  compact?: boolean;
}) {
  const queryClient = useQueryClient();
  const currentUser = useCurrentUser();
  const walletPayment = usePayBooking();
  const remainingSeconds = useCountdown(booking.paymentExpiresAt);
  const expired = remainingSeconds === 0;

  useEffect(() => {
    if (!expired) return;
    void queryClient.invalidateQueries({ queryKey: bookingQueryKeys.all });
    void queryClient.invalidateQueries({ queryKey: bookingQueryKeys.detail(booking.id) });
    void queryClient.invalidateQueries({ queryKey: recurringBookingQueryKeys.all });
  }, [booking.id, expired, queryClient]);

  if (!booking.sourceRecurringBookingId || booking.status !== "PENDING") {
    return null;
  }

  function pay() {
    if ((currentUser.data?.balance ?? 0) <= 0) {
      openWalletTopUpPanel({ returnPath: `/bookings/${booking.id}` });
      return;
    }
    walletPayment.mutate(booking.id);
  }

  return (
    <div className={compact
      ? "flex flex-wrap items-center justify-between gap-3 rounded-xl border border-amber-200 bg-amber-50 px-4 py-3 text-sm"
      : "rounded-2xl border border-amber-200 bg-amber-50 p-4 text-sm"}
    >
      <div className="flex min-w-0 items-center gap-2 text-amber-900">
        <TimerReset className="size-4 shrink-0" />
        <span className="font-bold">Còn {formatRemaining(remainingSeconds)} để thanh toán</span>
      </div>
      {expired ? (
        <span className="font-bold text-rose-700">Đang cập nhật trạng thái</span>
      ) : (
        <button
          type="button"
          onClick={pay}
          disabled={currentUser.isPending || walletPayment.isPending}
          className="inline-flex mt-3 min-h-10 items-center justify-center gap-2 rounded-lg bg-green-600 px-3 text-sm font-black text-white hover:bg-green-700 disabled:cursor-not-allowed disabled:opacity-60"
        >
          {walletPayment.isPending ? <LoaderCircle className="size-4 animate-spin" /> : <CreditCard className="size-4" />}
          Thanh toán
        </button>
      )}
      {walletPayment.error ? (
        <p className={compact ? "basis-full text-xs font-semibold text-rose-700" : "mt-3 text-xs font-semibold text-rose-700"}>
          {walletPayment.error.message}
        </p>
      ) : null}
    </div>
  );
}

function formatRemaining(seconds: number | null) {
  if (seconds === null) return "--:--";
  const minutes = Math.floor(seconds / 60);
  const rest = seconds % 60;
  return `${String(minutes).padStart(2, "0")}:${String(rest).padStart(2, "0")}`;
}
