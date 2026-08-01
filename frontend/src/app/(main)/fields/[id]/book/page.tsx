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
  searchParams,
}: {
  params: Promise<{ id: string }>;
  searchParams: Promise<{ mode?: string }>;
}) {
  const user = await requireUser();
  const query = await searchParams;
  const reservationMode = query.mode === "reservation";
  if (user.userType !== "CLIENT" && user.userType !== "EMPLOYEE" && user.userType !== "OWNER") return <AccessDenied />;
  if (reservationMode && user.userType !== "OWNER") return <AccessDenied />;
  const { id } = await params;
  const initialDate = todayInVietnam();
  const queryClient = await prefetchFieldBooking(id, initialDate);
  return (
    <div className="min-h-[70vh] bg-slate-50">
      <div className="mx-auto w-full max-w-[90rem] px-5 py-10 sm:px-8">
      <HydrationBoundary state={dehydrate(queryClient)}>
        <BookingForm fieldId={id} initialDate={initialDate} reservationMode={reservationMode} />
      </HydrationBoundary>
      </div>
    </div>
  );
}
