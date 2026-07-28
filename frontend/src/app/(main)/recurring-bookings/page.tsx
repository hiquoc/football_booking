import { PageHeading } from "@/components/ui/page-heading";
import { RecurringBookingList } from "@/components/bookings/recurring-booking-list";

export default function RecurringBookingsPage() {
  return (
    <div className="space-y-6">
      <PageHeading
        eyebrow="Đặt sân"
        title="Lịch đặt định kỳ"
        description="Quản lý các lịch đặt sân định kỳ của bạn."
      />
      <RecurringBookingList scope="my" />
    </div>
  );
}
