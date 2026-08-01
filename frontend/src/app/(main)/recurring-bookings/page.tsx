import { PageHeading } from "@/components/ui/page-heading";
import { RecurringBookingList } from "@/components/bookings/recurring-booking-list";
import { BackLink } from "@/components/ui/back-link";

export default function RecurringBookingsPage() {
  return (
    <div className="mx-auto w-full max-w-[90rem] space-y-6 px-5 py-8 sm:px-8">
      <BackLink href="/bookings">
        Lịch đặt của tôi
      </BackLink>
      <PageHeading
        eyebrow="Đặt sân"
        title="Lịch đặt định kỳ"
        description="Quản lý các lịch đặt sân định kỳ của bạn."
      />
      <RecurringBookingList scope="my" />
    </div>
  );
}
