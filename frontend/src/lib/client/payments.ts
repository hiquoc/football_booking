import type { CheckoutResponse, CreateCheckoutInput, Payment } from "@/lib/api/types";
import { jsonBody, requestJson } from "./http";
export function createCheckout(input: CreateCheckoutInput) {
  return requestJson<CheckoutResponse>("/api/payments/checkout", { method: "POST", ...jsonBody(input) });
}
export function fetchPayment(bookingId: string) {
  return requestJson<Payment>(`/api/payments/${encodeURIComponent(bookingId)}`);
}
