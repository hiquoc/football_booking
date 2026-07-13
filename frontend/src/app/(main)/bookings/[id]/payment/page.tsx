import type { Metadata } from "next";
import { PaymentContent } from "@/components/bookings/payment-content";
export const metadata: Metadata = { title: "Thanh toán lịch đặt" };
export default async function PaymentPage({
  params,
  searchParams,
}: {
  params: Promise<{ id: string }>;
  searchParams: Promise<{ checkout?: string }>;
}) {
  const query = await searchParams;
  return (
    <div className="min-h-[70vh] px-5 py-12">
      <PaymentContent bookingId={(await params).id} returned={Boolean(query.checkout)} />
    </div>
  );
}
