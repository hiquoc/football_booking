import type { Metadata } from "next";
import { BookingListContent } from "@/components/bookings/booking-list-content";
import { PageHeading } from "@/components/ui/page-heading";

export const metadata: Metadata = { title: "Lịch đặt của tôi" };

export default async function BookingsPage({
  searchParams,
}: {
  searchParams: Promise<{ page?: string }>;
}) {
  const requested = Number((await searchParams).page) || 1;
  const page = Math.max(0, requested - 1);
  return (
    <div className="mx-auto min-h-[70vh] w-full max-w-[90rem] px-5 py-12 sm:px-8">
      <PageHeading
        eyebrow="Quản lý trận đấu"
        title="Lịch đặt của tôi"
        description="Theo dõi trạng thái, thanh toán hoặc hủy các lịch đặt sân của bạn."
      />
      <div className="mt-8">
        <BookingListContent page={page} />
      </div>
    </div>
  );
}
