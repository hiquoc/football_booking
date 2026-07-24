"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { CheckCircle2, CreditCard, LoaderCircle, LockKeyhole, XCircle } from "lucide-react";
import { formatCurrency } from "@/lib/field-format";
import { useBooking } from "@/lib/hooks/use-bookings";
import { useCountdown } from "@/lib/hooks/use-countdown";
import { useCreateCheckout, usePayment } from "@/lib/hooks/use-payments";
import { useCurrentUser } from "@/lib/hooks/use-profile";
import { DataError, FormSkeleton } from "@/components/ui/data-state";

const TOP_UP_AMOUNTS = [20000, 30000, 40000, 50000] as const;

export function PaymentContent({ bookingId, returned }: { bookingId: string; returned: boolean }) {
  const router = useRouter();
  const booking = useBooking(bookingId);
  const bookingConfirmed = booking.data?.status === "CONFIRMED";
  const currentUser = useCurrentUser({ refetchInterval: bookingConfirmed ? false : 2000 });
  const payment = usePayment(bookingId, returned && !bookingConfirmed);
  const checkout = useCreateCheckout();
  const [selectedTopUpAmount, setSelectedTopUpAmount] = useState<number>(TOP_UP_AMOUNTS[0]);
  const bookingDeadline = booking.data
    ? (booking.data.paymentExpiresAt ?? new Date(new Date(booking.data.createdAt).getTime() + 5 * 60 * 1000).toISOString())
    : null;
  const remainingSeconds = useCountdown(bookingDeadline);
  const expired = remainingSeconds === 0;

  if (booking.isPending || currentUser.isPending || (returned && payment.isPending)) return <FormSkeleton />;
  if (booking.isError) return <DataError title="Không thể tải thông tin nạp ví" />;

  const data = booking.data;
  const status = payment.data?.status;

  if (data.status === "CONFIRMED") {
    return (
      <Result
        icon={<CheckCircle2 />}
        title="Lịch đặt sân đã được xác nhận"
        message="Ví đã được cập nhật, phí đặt sân đã được trừ và lịch đặt đã hoàn tất."
        onDone={() => router.push(`/bookings/${data.id}`)}
      />
    );
  }

  if (status === "SUCCESS") {
    return (
      <Result
        icon={<LoaderCircle className="animate-spin" />}
        title="Đang hoàn tất đặt sân"
        message="Ví đã được nạp tiền. Hệ thống đang tự động trừ phí đặt sân và xác nhận lịch đặt."
        onDone={() => router.push(`/bookings/${data.id}`)}
      />
    );
  }

  if (status === "FAILED" || status === "CANCELLED") {
    return (
      <Result
        icon={<XCircle />}
        title={status === "FAILED" ? "Nạp ví thất bại" : "Nạp ví đã hủy"}
        message="Không thể hoàn tất nạp ví. Bạn có thể tạo lại phiên nạp ví nếu lịch đặt chưa hết hạn."
        onDone={() => router.replace(`/bookings/${data.id}/payment`)}
      />
    );
  }

  if (returned && status === "PENDING") {
    return (
      <Result
        icon={<LoaderCircle className="animate-spin" />}
        title="Đang xác nhận nạp ví"
        message={`Hệ thống đang chờ kết quả nạp ví từ Stripe. Thời gian giữ lịch còn lại: ${formatRemaining(remainingSeconds)}.`}
        onDone={() => router.push(`/bookings/${data.id}`)}
      />
    );
  }

  if (returned && payment.isError) return <DataError title="Chưa thể xác nhận trạng thái nạp ví" />;
  if (data.status !== "PENDING") {
    return (
      <Result
        icon={<CheckCircle2 />}
        title="Lịch đặt sân không còn chờ thanh toán"
        message="Lịch đặt này không còn khoản thanh toán đang chờ."
        onDone={() => router.push(`/bookings/${data.id}`)}
      />
    );
  }

  const bookingPrice = Number(data.bookingPrice ?? data.platformBookingFee ?? 0);
  const walletBalance = currentUser.data?.balance ?? 0;
  const missingAmount = Math.max(bookingPrice - walletBalance, 0);

  if (missingAmount <= 0) {
    return (
      <Result
        icon={<LoaderCircle className="animate-spin" />}
        title="Đang xác nhận thanh toán bằng ví"
        message={`Ví của bạn đã đủ để thanh toán phí đặt sân. Hệ thống đang tự động trừ phí và xác nhận lịch. Thời gian giữ lịch còn lại: ${formatRemaining(remainingSeconds)}.`}
        onDone={() => router.push(`/bookings/${data.id}`)}
      />
    );
  }

  async function pay() {
    try {
      const result = await checkout.mutateAsync({ bookingId: data.id, amount: selectedTopUpAmount, currency: "VND", provider: "STRIPE" });
      window.location.assign(result.checkoutUrl);
    } catch {
      // Mutation state renders the error.
    }
  }

  return (
    <div className="mx-auto max-w-xl rounded-[2rem] border border-slate-200 bg-white p-7 shadow-xl sm:p-9">
      <span className="grid size-12 place-items-center rounded-2xl bg-sky-100 text-sky-700"><CreditCard className="size-6" /></span>
      <h1 className="mt-5 text-3xl font-black text-slate-950">Nạp ví thanh toán</h1>
      <p className="mt-2 text-sm text-slate-500">Lịch đặt {data.bookingCode} đã được tạm giữ. Stripe chỉ dùng để nạp tiền vào ví.</p>
      <div className="mt-7 rounded-2xl bg-slate-50 p-5">
        <div className="flex justify-between text-sm">
          <span className="text-slate-500">{data.fieldName} · {data.subFieldName}</span>
          <strong>{formatCurrency(Number(data.subFieldPrice ?? data.totalAmount))}</strong>
        </div>
        <div className="mt-3 flex justify-between text-sm">
          <span className="text-slate-500">Phí đặt sân</span>
          <strong>{formatCurrency(bookingPrice)}</strong>
        </div>
        <div className="mt-3 flex justify-between text-sm">
          <span className="text-slate-500">Số dư ví hiện tại</span>
          <strong>{formatCurrency(walletBalance)}</strong>
        </div>
        <div className="mt-3 flex justify-between text-sm">
          <span className="text-slate-500">Số tiền còn thiếu</span>
          <strong>{formatCurrency(missingAmount)}</strong>
        </div>
        <label className="mt-4 block border-t border-slate-200 pt-4 text-sm">
          <span className="mb-2 block font-semibold text-slate-500">Chọn số tiền nạp</span>
          <select
            value={selectedTopUpAmount}
            onChange={(event) => setSelectedTopUpAmount(Number(event.target.value))}
            disabled={checkout.isPending || expired}
            className="input-field"
          >
            {TOP_UP_AMOUNTS.map((value) => (
              <option key={value} value={value}>
                {formatCurrency(value)}
              </option>
            ))}
          </select>
        </label>
        <div className="mt-4 flex items-center justify-between border-t border-slate-200 pt-4 text-sm">
          <span className="font-semibold text-slate-500">Thời gian giữ lịch còn lại</span>
          <strong className={expired ? "text-rose-600" : "tabular-nums text-sky-700"}>{formatRemaining(remainingSeconds)}</strong>
        </div>
      </div>
      <p className="mt-5 flex gap-2 text-xs leading-5 text-slate-400">
        <LockKeyhole className="size-4 shrink-0" />
        Sau khi nạp ví thành công, hệ thống sẽ tự động trừ phí đặt sân và xác nhận lịch.
      </p>
      {checkout.error ? <p className="mt-4 rounded-xl bg-rose-50 p-3 text-sm text-rose-700">Không thể tạo phiên nạp ví. Vui lòng thử lại sau.</p> : null}
      <button onClick={pay} disabled={checkout.isPending || expired} className="action-button mt-6 w-full bg-sky-500 px-5 text-white hover:bg-sky-600 disabled:cursor-not-allowed disabled:opacity-50">
        {checkout.isPending ? <LoaderCircle className="size-4 animate-spin" /> : null}
        {expired ? "Đã hết thời gian giữ lịch" : `Nạp ví ${formatCurrency(selectedTopUpAmount)}`}
      </button>
    </div>
  );
}

function formatRemaining(seconds: number | null) {
  if (seconds === null) return "--:--";
  const minutes = Math.floor(seconds / 60);
  const rest = seconds % 60;
  return `${String(minutes).padStart(2, "0")}:${String(rest).padStart(2, "0")}`;
}

function Result({ icon, title, message, onDone }: { icon: React.ReactNode; title: string; message: string; onDone: () => void }) {
  return (
    <div className="mx-auto max-w-xl rounded-[2rem] border border-slate-200 bg-white p-10 text-center shadow-sm">
      <span className="mx-auto grid size-12 place-items-center text-sky-600 [&_svg]:size-12">{icon}</span>
      <h1 className="mt-4 text-2xl font-black">{title}</h1>
      <p className="mt-2 text-sm text-slate-500">{message}</p>
      <button onClick={onDone} className="action-button mt-6 bg-slate-950 px-5 text-white hover:bg-slate-800">Tiếp tục</button>
    </div>
  );
}
