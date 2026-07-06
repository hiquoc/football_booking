import type { Metadata } from "next";
import { PaymentContent } from "@/components/bookings/payment-content";
export const metadata: Metadata = { title: "Thanh toán booking" };
export default async function PaymentPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  return (
    <div className="min-h-[70vh] px-5 py-12">
      <PaymentContent bookingId={(await params).id} />
    </div>
  );
}
