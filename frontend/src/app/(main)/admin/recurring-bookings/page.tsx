import { PageHeading } from "@/components/ui/page-heading";
import { RecurringBookingList } from "@/components/bookings/recurring-booking-list";

export default function AdminRecurringBookingsPage() {
  return (
    <div className="space-y-6">
      <PageHeading
        eyebrow="Admin"
        title="Recurring bookings"
        description="Search, pause, resume, and cancel recurring bookings."
      />
      <RecurringBookingList scope="admin" />
    </div>
  );
}
