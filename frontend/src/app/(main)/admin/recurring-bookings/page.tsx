import { PageHeading } from "@/components/ui/page-heading";
import { RecurringBookingList } from "@/components/bookings/recurring-booking-list";

export default function AdminRecurringBookingsPage() {
  return (
    <div className="space-y-6">
      <PageHeading
        eyebrow="Quản trị"
        title="Lịch đặt định kỳ"
        description="Tìm kiếm, tạm dừng, tiếp tục và hủy lịch đặt định kỳ."
      />
      <RecurringBookingList scope="admin" />
    </div>
  );
}
