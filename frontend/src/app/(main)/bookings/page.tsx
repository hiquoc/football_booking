import type { Metadata } from "next";
import Link from "next/link";
import { BookingListContent } from "@/components/bookings/booking-list-content";
import { BackLink } from "@/components/ui/back-link";
import { PageHeading } from "@/components/ui/page-heading";

export const metadata: Metadata = { title: "Lịch đặt của tôi" };

export default async function BookingsPage({
  searchParams,
}: {
  searchParams: Promise<{ page?: string; bookingDate?: string; status?: string }>;
}) {
  const query = await searchParams;
  const requested = Number(query.page) || 1;
  const page = Math.max(0, requested - 1);
  const filters = {
    bookingDate: query.bookingDate?.trim() || undefined,
    status: query.status?.trim() || undefined,
  };
  return (
    <div className="min-h-[70vh] bg-slate-50">
      <section className="border-b border-slate-200 bg-white py-12 sm:py-16">
        <div className="mx-auto w-full max-w-[90rem] px-5 sm:px-8">
          <BackLink href="/" className="mb-5">
            Trang chủ
          </BackLink>
          <PageHeading
            eyebrow="Quản lý trận đấu"
            title="Lịch đặt của tôi"
            description="Theo dõi trạng thái, thanh toán hoặc hủy các lịch đặt sân của bạn."
            action={
              <Link
                href="/recurring-bookings"
                className="action-button bg-green-600 px-5 text-white"
              >
                Lịch đặt định kỳ
              </Link>
            }
          />
        </div>
      </section>
      <div className="mx-auto w-full max-w-[90rem] px-5 py-10 sm:px-8">
        <BookingListContent page={page} filters={filters} />
      </div>
    </div>
  );
}
