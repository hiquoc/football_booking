import type { Metadata } from "next";
import { BookingDetailContent } from "@/components/bookings/booking-detail-content";

export const metadata: Metadata = { title: "Chi tiết booking" };
export default async function BookingDetailPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  return (
    <div className="mx-auto min-h-[70vh] max-w-5xl px-5 py-12 sm:px-8">
      <BookingDetailContent bookingId={(await params).id} />
    </div>
  );
}
