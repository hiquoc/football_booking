import type { QueryClient } from "@tanstack/react-query";
import type { CheckoutResponse, CreateCheckoutInput, User } from "@/lib/api/types";
import { userQueryKeys } from "@/lib/query-keys";

const PENDING_TOP_UP_KEY = "football.pendingTopUp";

type PendingTopUp = {
  paymentId: string;
  bookingId?: string;
  amount: number;
};

function canUseStorage() {
  return typeof window !== "undefined" && Boolean(window.localStorage);
}

export function rememberPendingTopUp(
  checkout: CheckoutResponse,
  input: CreateCheckoutInput,
) {
  if (!canUseStorage()) return;
  window.localStorage.setItem(
    PENDING_TOP_UP_KEY,
    JSON.stringify({
      paymentId: checkout.paymentId,
      bookingId: input.bookingId,
      amount: input.amount,
    } satisfies PendingTopUp),
  );
}

export function clearPendingTopUp() {
  if (!canUseStorage()) return;
  window.localStorage.removeItem(PENDING_TOP_UP_KEY);
}

export function consumePendingTopUp(bookingId?: string) {
  if (!canUseStorage()) return null;
  const raw = window.localStorage.getItem(PENDING_TOP_UP_KEY);
  if (!raw) return null;

  try {
    const pending = JSON.parse(raw) as PendingTopUp;
    if (bookingId && pending.bookingId !== bookingId) return null;
    window.localStorage.removeItem(PENDING_TOP_UP_KEY);
    return pending;
  } catch {
    window.localStorage.removeItem(PENDING_TOP_UP_KEY);
    return null;
  }
}

export function addOptimisticBalance(queryClient: QueryClient, amount: number) {
  if (!Number.isFinite(amount) || amount <= 0) return;
  queryClient.setQueryData<User>(userQueryKeys.mePrivate, (old) =>
    old ? { ...old, balance: old.balance + amount } : old,
  );
}
