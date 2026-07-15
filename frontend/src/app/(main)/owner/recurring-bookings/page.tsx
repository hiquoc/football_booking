import { PageHeading } from "@/components/ui/page-heading";
import { RecurringBookingList } from "@/components/bookings/recurring-booking-list";

export default function OwnerRecurringBookingsPage() {
  return (
    <div className="space-y-6">
      <PageHeading
        eyebrow="Owner"
        title="Recurring bookings"
        description="View recurring bookings for your fields."
      />
      <RecurringBookingList scope="owner" />
    </div>
  );
}
