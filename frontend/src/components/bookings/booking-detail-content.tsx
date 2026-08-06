"use client";

import { useState } from "react";
import {
  CalendarDays,
  Clock3,
  Hash,
  LoaderCircle,
  MapPin,
  ReceiptText,
  UserRound,
  XCircle,
} from "lucide-react";
import { BookingStatusButton } from "@/components/bookings/booking-status-button";
import { BackLink } from "@/components/ui/back-link";
import { DataError, DetailSkeleton } from "@/components/ui/data-state";
import { RecurringPaymentDeadline } from "@/components/bookings/recurring-payment-deadline";
import { bookingEndDateTime, bookingStartDateTime, formatBookingDateTime } from "@/lib/booking-format";
import { formatCurrency } from "@/lib/field-format";
import { openWalletTopUpPanel } from "@/lib/client/wallet-top-up-panel";
import { useBookingDisplayStatus } from "@/lib/hooks/use-booking-display-status";
import { useBooking, useCancelBooking } from "@/lib/hooks/use-bookings";
import { useCurrentUser } from "@/lib/hooks/use-profile";

export function BookingDetailContent({
  bookingId,
  owner = false,
}: {
  bookingId: string;
  owner?: boolean;
}) {
  const booking = useBooking(bookingId);
  const currentUser = useCurrentUser();
  const cancelMutation = useCancelBooking(owner);
  const derivedStatus = useBookingDisplayStatus(booking.data);
  const [reason, setReason] = useState("");
  const [showCancel, setShowCancel] = useState(false);

  if (booking.isPending) return <DetailSkeleton />;
  if (booking.isError) return <DataError title="Không thể tải chi tiết lịch đặt" />;

  const data = booking.data;
  const displayStatus = derivedStatus ?? data.status;
  const canCancel = data.status === "PENDING" || data.status === "CONFIRMED";
  const fieldPrice = Number(data.subFieldPrice ?? 0);
  const bookingFee = Number(data.bookingPrice ?? data.platformBookingFee ?? 0);
  const walletBalance = currentUser.data?.balance ?? 0;
  const needsTopUp = walletBalance < bookingFee;
  const startText = formatBookingDateTime(bookingStartDateTime(data));
  const endText = formatBookingDateTime(bookingEndDateTime(data));
  const customerInfo = formatCustomerInfo(data.clientName, data.clientPhoneNumber);
  const recurringPending = Boolean(data.sourceRecurringBookingId) && data.status === "PENDING";

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
      <BackLink href={owner ? "/owner/bookings" : "/bookings"} className="mb-5">
        Quay lại danh sách
      </BackLink>

      <div className="grid gap-7 lg:grid-cols-[minmax(0,1.55fr)_minmax(22rem,0.75fr)]">
        <main className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
          <header className="border-b border-slate-200 bg-white p-6 sm:p-8">
            <div className="min-w-0">
              <BookingStatusButton status={displayStatus} />
              <p className="mt-5 ml-3 inline-flex items-center gap-1 text-xs font-bold uppercase tracking-wider text-slate-400">
                <Hash className="size-3.5" />
                {data.bookingCode}
              </p>
              <h1 className="mt-2 text-4xl font-black leading-tight text-slate-950">
                {data.fieldName}
              </h1>
              <p className="mt-3 flex items-center gap-2 text-base font-semibold text-slate-600">
                <MapPin className="size-4 text-green-600" />
                {data.subFieldName}
              </p>
            </div>
          </header>

          <section className="p-6 sm:p-8">
            <h2 className="text-lg font-black text-slate-950">Thông tin lịch đặt</h2>
            <div className="mt-5 grid gap-5 sm:grid-cols-2">
              <Info icon={<CalendarDays />} label="Bắt đầu" value={startText} />
              <Info icon={<Clock3 />} label="Kết thúc" value={endText} />
              <Info
                icon={<Clock3 />}
                label="Tạo lúc"
                value={formatBookingDateTime(data.createdAt)}
              />
              <Info
                icon={<ReceiptText />}
                label="Ghi chú"
                value={data.note || "Không có ghi chú"}
              />
            </div>

            {/* <div className="mt-7 rounded-2xl border border-slate-200 bg-slate-50 p-5">
              <h3 className="text-base font-black text-slate-950">Chi phí đặt sân</h3>
              <div className="mt-4 space-y-3 text-sm">
                <PriceLine label="Thanh toán tại sân" value={fieldPrice} />
                <PriceLine label="Phí đặt lịch" value={bookingFee} />
                <div className="flex items-center justify-between border-t border-slate-200 pt-3">
                  <span className="font-semibold text-slate-600">Thanh toán qua ví</span>
                  <strong className="text-lg text-slate-950">
                    {formatCurrency(bookingFee)}
                  </strong>
                </div>
              </div>
            </div> */}

            {data.cancellationReason ? (
              <div className="mt-6 rounded-2xl border border-rose-200 bg-white p-4 text-sm text-rose-700">
                <strong>Lý do hủy:</strong> {data.cancellationReason}
              </div>
            ) : null}
          </section>
        </main>

        <aside className="h-fit rounded-2xl border border-slate-200 bg-white p-6 text-slate-900 shadow-sm lg:sticky lg:top-24">
          <section>
            <h3 className="text-xs font-black uppercase tracking-wider text-slate-500">
              Thao tác
            </h3>

            {recurringPending && !owner ? (
              <div className="mt-4">
                <RecurringPaymentDeadline booking={data} />
              </div>
            ) : null}

            {data.status === "PENDING" && !owner && !recurringPending && currentUser.isPending ? (
              <button
                disabled
                className="action-button mt-4 min-h-14 w-full cursor-not-allowed bg-slate-300 text-base text-white"
              >
                <LoaderCircle className="size-5 animate-spin" />
                Đang kiểm tra ví
              </button>
            ) : null}
            {data.status === "PENDING" && !owner && !recurringPending && !currentUser.isPending && needsTopUp ? (
              <button
                type="button"
                onClick={() => openWalletTopUpPanel({ returnPath: `/bookings/${data.id}` })}
                className="action-button mt-4 min-h-14 w-full bg-green-600 text-base text-white hover:bg-green-700"
              >
                Thanh toán
              </button>
            ) : null}
            {data.status === "PENDING" && !owner && !recurringPending && !currentUser.isPending && !needsTopUp ? (
              <button
                disabled
                className="action-button mt-4 min-h-14 w-full cursor-not-allowed bg-slate-900 text-base text-white opacity-80"
              >
                <LoaderCircle className="size-5 animate-spin" />
                Đang thanh toán
              </button>
            ) : null}
            {canCancel ? (
              <button
                onClick={() => setShowCancel((value) => !value)}
                className="action-button mt-3 min-h-14 w-full border border-rose-200 bg-rose-500 text-base text-white hover:bg-rose-600"
              >
                <XCircle className="size-5" />
                Hủy lịch đặt
              </button>
            ) : (
              <p className="mt-4 text-sm leading-6 text-slate-500">
                Lịch đặt này không còn thao tác cần xử lý.
              </p>
            )}
          </section>

          {showCancel ? (
            <div className="mt-4 rounded-2xl border border-slate-200 bg-white p-4">
              <label className="text-xs font-bold text-slate-600">Lý do hủy</label>
              <textarea
                value={reason}
                onChange={(event) => setReason(event.target.value)}
                rows={3}
                maxLength={255}
                placeholder="Nhập lý do (không bắt buộc)"
                className="mt-2 w-full resize-none rounded-xl border border-slate-200 p-3 text-sm outline-none focus:border-green-500 focus:ring-4 focus:ring-green-100"
              />
              {cancelMutation.error ? (
                <p className="mt-2 text-xs font-semibold text-rose-600">
                  {cancelMutation.error.message}
                </p>
              ) : null}
              <button
                onClick={cancel}
                disabled={cancelMutation.isPending}
                className="action-button mt-3 min-h-14 w-full bg-rose-500 text-base text-white hover:bg-rose-600"
              >
                {cancelMutation.isPending ? (
                  <LoaderCircle className="size-5 animate-spin" />
                ) : null}
                Xác nhận hủy
              </button>
            </div>
          ) : null}

          <div className="my-5 h-px bg-slate-200" />
          <section>
            <h3 className="text-xs font-black uppercase tracking-wider text-slate-500">
              Tóm tắt
            </h3>
            <div className="mt-4 space-y-3">
              <SummaryLine label="Mã lịch đặt" value={data.bookingCode} />
              <SummaryLine label="Sân con" value={data.subFieldName} />
              {owner ? (
                <SummaryLine
                  label="Khách hàng"
                  value={customerInfo}
                  icon={<UserRound className="size-4" />}
                />
              ) : null}
              <SummaryLine label="Bắt đầu" value={startText} />
              <SummaryLine label="Kết thúc" value={endText} />
            </div>
          </section>

          <div className="my-5 h-px bg-slate-200" />
          <section>
            <h3 className="text-xs font-black uppercase tracking-wider text-slate-500">
              Thanh toán
            </h3>
            <div className="mt-4 space-y-3 text-sm">
              <PriceLine label="Tại sân" value={fieldPrice} />
              <PriceLine label="Phí đặt lịch" value={bookingFee} />
              <div className="flex items-center justify-between rounded-xl bg-slate-50 px-3 py-3">
                <span className="font-bold text-slate-600">Thanh toán qua ví</span>
                <strong className="text-slate-950">{formatCurrency(bookingFee)}</strong>
              </div>
            </div>
          </section>
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
    <div className="flex gap-3 rounded-2xl border border-slate-200 p-4">
      <span className="mt-0.5 text-green-600 [&_svg]:size-5">{icon}</span>
      <div>
        <p className="text-xs font-bold uppercase tracking-wider text-slate-400">
          {label}
        </p>
        <p className="mt-1 font-semibold text-slate-700">{value}</p>
      </div>
    </div>
  );
}

function PriceLine({ label, value }: { label: string; value: number }) {
  return (
    <div className="flex items-center justify-between gap-4">
      <span className="font-semibold text-slate-600">{label}</span>
      <strong className="shrink-0 text-slate-950">{formatCurrency(value)}</strong>
    </div>
  );
}

function SummaryLine({
  label,
  value,
  icon,
}: {
  label: string;
  value: string;
  icon?: React.ReactNode;
}) {
  return (
    <div className="flex items-start justify-between gap-4 text-sm">
      <span className="inline-flex items-center gap-2 font-semibold text-slate-500">
        {icon ? <span className="text-green-600">{icon}</span> : null}
        {label}
      </span>
      <strong className="max-w-44 text-right font-bold text-slate-900">
        {value}
      </strong>
    </div>
  );
}

function formatCustomerInfo(name: string | null, phone: string | null) {
  if (name && phone) return `${name} (${phone})`;
  if (name) return name;
  if (phone) return phone;
  return "Không xác định";
}
