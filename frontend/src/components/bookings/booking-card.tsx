"use client";

import Link from "next/link";
import { CalendarDays, Clock3, MapPin } from "lucide-react";
import type { Booking } from "@/lib/api/types";
import { formatBookingDate, getBookingStatus } from "@/lib/booking-format";
import { formatCurrency, formatTime } from "@/lib/field-format";
import { useBookingDisplayStatus } from "@/lib/hooks/use-booking-display-status";

export function BookingCard({
  booking,
  owner = false,
  action,
}: {
  booking: Booking;
  owner?: boolean;
  action?: React.ReactNode;
}) {
  const status = getBookingStatus(useBookingDisplayStatus(booking) ?? booking.status);
  return (
    <article className="rounded-[1.5rem] border border-slate-200 bg-white p-5 shadow-sm transition hover:border-sky-200 hover:shadow-md">
      <div className="flex flex-col justify-between gap-4 sm:flex-row sm:items-start">
        <div>
          <div className="flex flex-wrap items-center gap-2">
            <span
              className={`rounded-full px-3 py-1 text-xs font-black ${status.className}`}
            >
              {status.label}
            </span>
            <span className="text-xs font-bold text-slate-400">
              {booking.bookingCode}
            </span>
          </div>
          <h2 className="mt-3 text-xl font-black text-slate-700">
            {booking.fieldName}
          </h2>
          <p className="mt-1 flex items-center gap-2 text-sm text-slate-500">
            <MapPin className="size-4 text-sky-600" />{" "}
            {booking.subFieldName}
          </p>
        </div>
        <strong className="text-lg text-slate-950">
          {formatCurrency(Number(booking.totalAmount))}
        </strong>
      </div>
      <div className="mt-5 flex flex-wrap gap-x-6 gap-y-2 border-t border-slate-100 pt-4 text-sm text-slate-500">
        <span className="inline-flex items-center gap-2">
          <CalendarDays className="size-4" />{" "}
          {formatBookingDate(booking.bookingDate)}
        </span>
        <span className="inline-flex items-center gap-2">
          <Clock3 className="size-4" /> {formatTime(booking.startTime)} –{" "}
          {formatTime(booking.endTime)}
        </span>
        <span className="ml-auto flex items-center gap-3">
          {action}
          {!owner ? (
            <Link
              href={`/bookings/${booking.id}`}
              className="rounded-lg px-2 py-1 font-black text-sky-600 hover:bg-sky-50"
            >
              Xem chi tiết →
            </Link>
          ) : null}
        </span>
      </div>
    </article>
  );
}
