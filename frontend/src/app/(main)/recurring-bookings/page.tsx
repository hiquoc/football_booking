import { PageHeading } from "@/components/ui/page-heading";
import { RecurringBookingList } from "@/components/bookings/recurring-booking-list";

export default function RecurringBookingsPage() {
  return (
    <div className="space-y-6">
      <PageHeading
        eyebrow="Bookings"
        title="Recurring bookings"
        description="Manage your recurring reservations."
      />
      <RecurringBookingList scope="my" />
    </div>
  );
}
