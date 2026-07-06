"use client";

import { useRouter } from "next/navigation";
import { useState } from "react";
import {
  CheckCircle2,
  CreditCard,
  LoaderCircle,
  LockKeyhole,
} from "lucide-react";
import { bookingStatus } from "@/lib/booking-format";
import { formatCurrency } from "@/lib/field-format";
import { useBooking, useMockPayment } from "@/lib/hooks/use-bookings";
import { DataError, ListSkeleton } from "@/components/ui/data-state";

export function PaymentContent({ bookingId }: { bookingId: string }) {
  const router = useRouter();
  const booking = useBooking(bookingId);
  const payment = useMockPayment();
  const [leaving, setLeaving] = useState(false);
  if (booking.isPending) return <ListSkeleton count={2} />;
  if (booking.isError)
    return <DataError title="Không thể tải thông tin thanh toán" />;
  const data = booking.data;

  if (data.status !== "PENDING" && !leaving)
    return (
      <div className="rounded-[2rem] border border-slate-200 bg-white p-10 text-center">
        <CheckCircle2 className="mx-auto size-12 text-sky-500" />
        <h1 className="mt-4 text-2xl font-black">
          Booking {bookingStatus[data.status].label.toLowerCase()}
        </h1>
        <button
          onClick={() => router.push(`/bookings/${data.id}`)}
          className="action-button mt-6 bg-slate-950 px-5 text-white hover:bg-slate-800"
        >
          Xem chi tiết
        </button>
      </div>
    );

  async function pay() {
    try {
      setLeaving(true);
      await payment.mutateAsync(data.id);
      window.location.assign(`/bookings/${data.id}`);
    } catch {
      setLeaving(false);
      /* Rendered below. */
    }
  }
  return (
    <div className="mx-auto max-w-xl rounded-[2rem] border border-slate-200 bg-white p-7 shadow-xl sm:p-9">
      <span className="grid size-12 place-items-center rounded-2xl bg-sky-100 text-sky-700">
        <CreditCard className="size-6" />
      </span>
      <h1 className="mt-5 text-3xl font-black text-slate-950">
        Xác nhận thanh toán
      </h1>
      <p className="mt-2 text-sm text-slate-500">
        Thanh toán mô phỏng cho booking {data.bookingCode}
      </p>
      <div className="mt-7 rounded-2xl bg-slate-50 p-5">
        <div className="flex justify-between text-sm">
          <span className="text-slate-500">
            {data.fieldName} · {data.subFieldName}
          </span>
          <strong>{formatCurrency(Number(data.totalAmount))}</strong>
        </div>
      </div>
      <p className="mt-5 flex gap-2 text-xs leading-5 text-slate-400">
        <LockKeyhole className="size-4 shrink-0" /> Đây là thanh toán mô phỏng
        phục vụ môi trường phát triển, không phát sinh giao dịch thật.
      </p>
      {payment.error ? (
        <p className="mt-4 rounded-xl bg-rose-50 p-3 text-sm text-rose-700">
          {payment.error.message}
        </p>
      ) : null}
      <button
        onClick={pay}
        disabled={payment.isPending || leaving}
        className="action-button mt-6 w-full bg-sky-500 px-5 text-white hover:bg-sky-600"
      >
        {payment.isPending ? (
          <LoaderCircle className="size-4 animate-spin" />
        ) : null}{" "}
        Xác nhận thanh toán {formatCurrency(Number(data.totalAmount))}
      </button>
    </div>
  );
}
