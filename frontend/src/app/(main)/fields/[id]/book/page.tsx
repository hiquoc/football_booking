import type { Metadata } from "next";
import { dehydrate, HydrationBoundary } from "@tanstack/react-query";
import { BookingForm, type BookingFormInitialSelection } from "@/components/bookings/booking-form";
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
  searchParams: Promise<Record<string, string | string[] | undefined>>;
}) {
  const user = await requireUser();
  const query = await searchParams;
  const reservationMode = query.mode === "reservation";
  if (user.userType !== "CLIENT" && user.userType !== "EMPLOYEE" && user.userType !== "OWNER") return <AccessDenied />;
  if (reservationMode && user.userType !== "OWNER") return <AccessDenied />;
  const { id } = await params;
  const initialDate = todayInVietnam();
  const initialSelection = bookingInitialSelection(query, initialDate);
  const queryClient = await prefetchFieldBooking(id, initialDate);
  return (
    <div className="min-h-[70vh] bg-slate-50">
      <div className="mx-auto w-full max-w-[90rem] px-5 py-10 sm:px-8">
      <HydrationBoundary state={dehydrate(queryClient)}>
        <BookingForm
          fieldId={id}
          initialDate={initialDate}
          initialSelection={initialSelection}
          reservationMode={reservationMode}
        />
      </HydrationBoundary>
      </div>
    </div>
  );
}

function bookingInitialSelection(
  query: Record<string, string | string[] | undefined>,
  initialDate: string,
): BookingFormInitialSelection {
  const date = first(query.date);
  return {
    date: date && /^\d{4}-\d{2}-\d{2}$/.test(date) && date >= initialDate ? date : initialDate,
    subFieldId: first(query.subFieldId) ?? "",
    slot: first(query.slot) ?? "",
    duration: positiveInteger(first(query.duration)) ?? undefined,
    recurringEnabled: first(query.recurring) === "1",
    recurringIntervalDays: boundedInteger(first(query.intervalDays), 1, 7) ?? undefined,
    recurringEndDate: first(query.endDate) ?? "",
  };
}

function first(value: string | string[] | undefined) {
  return Array.isArray(value) ? value[0] : value;
}

function positiveInteger(value: string | undefined) {
  if (!value || !/^\d+$/.test(value)) return null;
  const parsed = Number(value);
  return parsed > 0 ? parsed : null;
}

function boundedInteger(value: string | undefined, min: number, max: number) {
  const parsed = positiveInteger(value);
  return parsed !== null && parsed >= min && parsed <= max ? parsed : null;
}
