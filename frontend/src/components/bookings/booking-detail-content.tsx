"use client";

import Link from "next/link";
import { useState } from "react";
import {
  CalendarDays,
  CheckCircle2,
  Clock3,
  Hash,
  LoaderCircle,
  MapPin,
  ReceiptText,
  XCircle,
} from "lucide-react";
import { BackLink } from "@/components/ui/back-link";
import { DataError, ListSkeleton } from "@/components/ui/data-state";
import type { BookingDisplayStatus } from "@/lib/booking-format";
import { bookingStatus, formatBookingDate } from "@/lib/booking-format";
import { formatCurrency, formatTime } from "@/lib/field-format";
import { useBookingDisplayStatus } from "@/lib/hooks/use-booking-display-status";
import { useBooking, useCancelBooking } from "@/lib/hooks/use-bookings";

const statusDescriptions: Record<BookingDisplayStatus, string> = {
  PENDING: "Booking đang giữ chỗ và chờ xác nhận thanh toán.",
  CONFIRMED: "Lịch đặt đã được xác nhận thành công.",
  IN_PROGRESS: "Trận đấu đang diễn ra theo khung giờ đã đặt.",
  CANCELLED: "Lịch đặt đã bị hủy và không còn hiệu lực.",
  COMPLETED: "Lịch đặt đã hoàn thành.",
  EXPIRED: "Booking đã hết thời gian thanh toán.",
};

export function BookingDetailContent({
  bookingId,
  owner = false,
}: {
  bookingId: string;
  owner?: boolean;
}) {
  const booking = useBooking(bookingId);
  const cancelMutation = useCancelBooking(owner);
  const derivedStatus = useBookingDisplayStatus(booking.data);
  const [reason, setReason] = useState("");
  const [showCancel, setShowCancel] = useState(false);

  if (booking.isPending) return <ListSkeleton count={2} />;
  if (booking.isError)
    return <DataError title="Không thể tải chi tiết booking" />;

  const data = booking.data;
  const displayStatus = derivedStatus ?? data.status;
  const status = bookingStatus[displayStatus];
  const canCancel = data.status === "PENDING" || data.status === "CONFIRMED";

  async function cancel() {
    try {
      await cancelMutation.mutateAsync({
        id: bookingId,
        reason: reason.trim() || undefined,
      });
      setShowCancel(false);
    } catch {
      // Mutation state renders the error and restores optimistic cache data.
    }
  }

  return (
    <div>
      <BackLink
        href={owner ? "/owner/bookings" : "/bookings"}
        className="mb-5"
      >
        Quay lại danh sách
      </BackLink>

      <div className="grid gap-7 lg:grid-cols-[minmax(0,1fr)_22rem]">
        <main className="overflow-hidden rounded-[2rem] border border-slate-200 bg-white shadow-sm">
          <header className="border-b border-slate-100 bg-gradient-to-br from-sky-50 via-white to-emerald-50 p-6 sm:p-8">
            <div className="flex flex-wrap items-start justify-between gap-5">
              <div>
                <span
                  className={`rounded-full px-3 py-1 text-xs font-black ${status.className}`}
                >
                  {status.label}
                </span>
                <p className="mt-5 inline-flex items-center gap-1 text-xs font-bold uppercase tracking-wider text-slate-400">
                  <Hash className="size-3.5" />
                  {data.bookingCode}
                </p>
                <h1 className="mt-1 text-3xl font-black text-slate-950">
                  {data.fieldName}
                </h1>
                <p className="mt-2 flex items-center gap-2 text-slate-500">
                  <MapPin className="size-4 text-sky-600" />
                  {data.subFieldName}
                </p>
              </div>
            </div>
          </header>

          <section className="p-6 sm:p-8">
            <h2 className="text-lg font-black text-slate-950">
              Thông tin lịch đặt
            </h2>
            <div className="mt-5 grid gap-5 sm:grid-cols-2">
              <Info
                icon={<CalendarDays />}
                label="Ngày thi đấu"
                value={formatBookingDate(data.bookingDate)}
              />
              <Info
                icon={<Clock3 />}
                label="Thời gian"
                value={`${formatTime(data.startTime)} - ${formatTime(data.endTime)} (${data.durationMinutes} phút)`}
              />
              <div className="sm:col-span-2">
                <Info
                  icon={<ReceiptText />}
                  label="Ghi chú"
                  value={data.note || "Không có ghi chú"}
                />
              </div>
            </div>

            <div className="mt-7 flex items-center justify-between gap-4 rounded-2xl border border-slate-200 bg-slate-50 p-5">
              <div>
                <p className="text-xs font-bold uppercase tracking-wider text-slate-800">
                  Thành tiền
                </p>
              </div>
              <strong className="shrink-0 text-xl text-slate-950">
                {formatCurrency(Number(data.totalAmount))}
              </strong>
            </div>

            {data.cancellationReason ? (
              <div className="mt-6 rounded-2xl border border-rose-100 bg-rose-50 p-4 text-sm text-rose-700">
                <strong>Lý do hủy:</strong> {data.cancellationReason}
              </div>
            ) : null}
          </section>
        </main>

        <aside className="h-fit rounded-[2rem] border border-sky-100 bg-sky-50 p-6 text-slate-900 lg:sticky lg:top-24">
          <div className="flex items-start gap-3">
            <CheckCircle2 className="mt-0.5 size-5 shrink-0 text-sky-600" />
            <div>
              <h2 className="font-black">{status.label}</h2>
              <p className="mt-1 text-sm leading-5 text-slate-500">
                {statusDescriptions[displayStatus]}
              </p>
            </div>
          </div>
          <div className="my-5 h-px bg-sky-100" />
          <h3 className="text-xs font-black uppercase tracking-wider text-slate-500">
            Thao tác
          </h3>

          {data.status === "PENDING" && !owner ? (
            <Link
              href={`/bookings/${data.id}/payment`}
              className="action-button mt-4 w-full bg-sky-500 text-white hover:bg-sky-600"
            >
              Thanh toán ngay
            </Link>
          ) : null}
          {canCancel ? (
            <button
              onClick={() => setShowCancel((value) => !value)}
              className="action-button mt-3 w-full border border-rose-200 bg-rose-500 text-white hover:bg-rose-600"
            >
              <XCircle className="size-4" /> Hủy lịch đặt
            </button>
          ) : (
            <p className="mt-4 text-sm leading-6 text-slate-500">
              Booking này không còn thao tác cần xử lý.
            </p>
          )}

          {showCancel ? (
            <div className="mt-4 rounded-2xl bg-white p-4">
              <label className="text-xs font-bold text-slate-600">
                Lý do hủy
              </label>
              <textarea
                value={reason}
                onChange={(event) => setReason(event.target.value)}
                rows={3}
                maxLength={255}
                placeholder="Nhập lý do (không bắt buộc)"
                className="mt-2 w-full resize-none rounded-xl border border-slate-200 p-3 text-sm outline-none focus:border-sky-400"
              />
              {cancelMutation.error ? (
                <p className="mt-2 text-xs font-semibold text-rose-600">
                  {cancelMutation.error.message}
                </p>
              ) : null}
              <button
                onClick={cancel}
                disabled={cancelMutation.isPending}
                className="action-button mt-3 w-full bg-rose-500 text-white hover:bg-rose-600"
              >
                {cancelMutation.isPending ? (
                  <LoaderCircle className="size-4 animate-spin" />
                ) : null}{" "}
                Xác nhận hủy
              </button>
            </div>
          ) : null}
        </aside>
      </div>
    </div>
  );
}

function Info({
  icon,
  label,
  value,
}: {
  icon: React.ReactNode;
  label: string;
  value: string;
}) {
  return (
    <div className="flex gap-3 rounded-2xl border border-slate-100 p-4">
      <span className="mt-0.5 text-sky-600 [&_svg]:size-5">{icon}</span>
      <div>
        <p className="text-xs font-bold uppercase tracking-wider text-slate-400">
          {label}
        </p>
        <p className="mt-1 font-semibold text-slate-700">{value}</p>
      </div>
    </div>
  );
}
