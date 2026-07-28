import { PageHeading } from "@/components/ui/page-heading";
import { RecurringBookingList } from "@/components/bookings/recurring-booking-list";

export default function OwnerRecurringBookingsPage() {
  return (
    <div className="space-y-6">
      <PageHeading
        eyebrow="Chủ sân"
        title="Lịch đặt định kỳ"
        description="Xem các lịch đặt định kỳ tại sân của bạn."
      />
      <RecurringBookingList scope="owner" />
    </div>
  );
}
