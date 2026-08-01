import type { Metadata } from "next";
import { BookingDetailContent } from "@/components/bookings/booking-detail-content";

export const metadata: Metadata = { title: "Chi tiết lịch đặt" };
export default async function BookingDetailPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  return (
    <div className="min-h-[70vh] bg-slate-50">
      <div className="mx-auto w-full max-w-[90rem] px-5 py-10 sm:px-8 sm:py-12">
        <BookingDetailContent bookingId={(await params).id} />
      </div>
    </div>
  );
}
