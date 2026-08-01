import { PageHeading } from "@/components/ui/page-heading";
import { RecurringBookingList } from "@/components/bookings/recurring-booking-list";
import { BackLink } from "@/components/ui/back-link";

export default function OwnerRecurringBookingsPage() {
  return (
    <div className="space-y-6">
      <BackLink href="/owner">
        Quản lý sân
      </BackLink>
      <PageHeading
        eyebrow="Chủ sân"
        title="Lịch đặt định kỳ"
        description="Xem các lịch đặt định kỳ tại sân của bạn."
      />
      <RecurringBookingList scope="owner" />
    </div>
  );
}
