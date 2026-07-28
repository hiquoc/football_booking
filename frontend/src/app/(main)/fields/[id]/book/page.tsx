import type { Metadata } from "next";
import { dehydrate, HydrationBoundary } from "@tanstack/react-query";
import { BookingForm } from "@/components/bookings/booking-form";
import { prefetchFieldBooking } from "@/lib/server/field-query-cache";
import { requireUser } from "@/lib/server/guards";
import { AccessDenied } from "@/components/ui/access-denied";

export const metadata: Metadata = { title: "Đặt sân" };

function todayInVietnam() {
  return new Date().toLocaleDateString("en-CA", {
    timeZone: "Asia/Ho_Chi_Minh",
  });
}

export default async function BookFieldPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const user = await requireUser();
  if (user.userType !== "CLIENT" && user.userType !== "EMPLOYEE") return <AccessDenied />;
  const { id } = await params;
  const initialDate = todayInVietnam();
  const queryClient = await prefetchFieldBooking(id, initialDate);
  return (
    <div className="mx-auto w-full max-w-[90rem] px-5 py-10 sm:px-8">
      <HydrationBoundary state={dehydrate(queryClient)}>
        <BookingForm fieldId={id} initialDate={initialDate} />
      </HydrationBoundary>
    </div>
  );
}
