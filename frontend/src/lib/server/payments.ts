import "server-only";
import type { CheckoutResponse, CreateCheckoutInput, Payment } from "@/lib/api/types";
import { authenticatedGatewayRequest } from "./authenticated-gateway";
export function createPaymentCheckout(input: CreateCheckoutInput) {
  return authenticatedGatewayRequest<CheckoutResponse>("/api/v1/payments/checkout", {
    method: "POST", body: JSON.stringify(input),
  });
}
export function getPayment(bookingId: string) {
  return authenticatedGatewayRequest<Payment>(`/api/v1/payments/${encodeURIComponent(bookingId)}`, { cache: "no-store" });
}
