import type { Metadata } from "next";
import Link from "next/link";
import { BookingListContent } from "@/components/bookings/booking-list-content";
import { PageHeading } from "@/components/ui/page-heading";

export const metadata: Metadata = { title: "Lich dat cua toi" };

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
        eyebrow="Quan ly tran dau"
        title="Lich dat cua toi"
        description="Theo doi trang thai, thanh toan hoac huy cac lich dat san cua ban."
        action={
          <Link
            href="/recurring-bookings"
            className="action-button bg-sky-500 px-4 text-white hover:bg-sky-600"
          >
            Lich dat dinh ky
          </Link>
        }
      />
      <div className="mt-8">
        <BookingListContent page={page} />
      </div>
    </div>
  );
}
