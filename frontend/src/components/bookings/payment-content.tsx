"use client";

import { useRouter } from "next/navigation";
import { CheckCircle2, CreditCard, LoaderCircle, LockKeyhole, XCircle } from "lucide-react";
import { formatCurrency } from "@/lib/field-format";
import { useBooking } from "@/lib/hooks/use-bookings";
import { useCreateCheckout, usePayment } from "@/lib/hooks/use-payments";
import { useCountdown } from "@/lib/hooks/use-countdown";
import { DataError, FormSkeleton } from "@/components/ui/data-state";

export function PaymentContent({ bookingId, returned }: { bookingId: string; returned: boolean }) {
  const router = useRouter();
  const booking = useBooking(bookingId);
  const payment = usePayment(bookingId);
  const checkout = useCreateCheckout();
  const bookingDeadline = booking.data
    ? new Date(new Date(booking.data.createdAt).getTime() + 35 * 60 * 1000).toISOString()
    : null;
  const remainingSeconds = useCountdown(payment.data?.expiresAt ?? bookingDeadline);
  const expired = remainingSeconds === 0;

  if (booking.isPending || (returned && payment.isPending)) return <FormSkeleton />;
  if (booking.isError) return <DataError title="Không thể tải thông tin thanh toán" />;
  const data = booking.data;
  const status = payment.data?.status;

  if (status === "SUCCESS") return <Result icon={<CheckCircle2 />} title="Thanh toán thành công"
      message="Thanh toán đã được Stripe xác nhận. Lịch đặt sân đang được cập nhật." onDone={() => router.push(`/bookings/${data.id}`)} />;
  if (status === "FAILED" || status === "CANCELLED") return <Result icon={<XCircle />}
      title={status === "FAILED" ? "Thanh toán thất bại" : "Thanh toán đã hủy"}
      message="Không thể hoàn tất thanh toán. Bạn có thể tạo lại phiên thanh toán." onDone={() => router.replace(`/bookings/${data.id}/payment`)} />;
  if (returned && status === "PENDING") return <Result icon={<LoaderCircle className="animate-spin" />} title="Đang xác nhận thanh toán"
      message={`Hệ thống đang chờ webhook đã xác minh từ Stripe. Thời gian còn lại: ${formatRemaining(remainingSeconds)}.`} onDone={() => router.push(`/bookings/${data.id}`)} />;
  if (returned && payment.isError) return <DataError title="Chưa thể xác nhận trạng thái thanh toán" />;
  if (data.status !== "PENDING") return <Result icon={<CheckCircle2 />} title="Lịch đặt sân đã được xác nhận"
      message="Lịch đặt này không còn khoản thanh toán đang chờ." onDone={() => router.push(`/bookings/${data.id}`)} />;

  async function pay() {
    try {
      const result = await checkout.mutateAsync({ bookingId: data.id, amount: Number(data.totalAmount), currency: "VND", provider: "STRIPE" });
      window.location.assign(result.checkoutUrl);
    } catch { /* Mutation state renders the error. */ }
  }

  return <div className="mx-auto max-w-xl rounded-[2rem] border border-slate-200 bg-white p-7 shadow-xl sm:p-9">
    <span className="grid size-12 place-items-center rounded-2xl bg-sky-100 text-sky-700"><CreditCard className="size-6" /></span>
    <h1 className="mt-5 text-3xl font-black text-slate-950">Thanh toán lịch đặt</h1>
    <p className="mt-2 text-sm text-slate-500">Thanh toán an toàn qua Stripe Test Mode cho lịch đặt {data.bookingCode}</p>
    <div className="mt-7 rounded-2xl bg-slate-50 p-5"><div className="flex justify-between text-sm">
      <span className="text-slate-500">{data.fieldName} · {data.subFieldName}</span><strong>{formatCurrency(Number(data.totalAmount))}</strong>
    </div><div className="mt-4 flex items-center justify-between border-t border-slate-200 pt-4 text-sm">
      <span className="font-semibold text-slate-500">Thời gian thanh toán còn lại</span>
      <strong className={expired ? "text-rose-600" : "tabular-nums text-sky-700"}>{formatRemaining(remainingSeconds)}</strong>
    </div></div>
    <p className="mt-5 flex gap-2 text-xs leading-5 text-slate-400"><LockKeyhole className="size-4 shrink-0" />
      Kết quả thanh toán chỉ được xác nhận sau khi hệ thống nhận kết quả hợp lệ từ Stripe.</p>
    {checkout.error ? <p className="mt-4 rounded-xl bg-rose-50 p-3 text-sm text-rose-700">Không thể tạo phiên thanh toán. Vui lòng thử lại sau.</p> : null}
    <button onClick={pay} disabled={checkout.isPending || expired} className="action-button mt-6 w-full bg-sky-500 px-5 text-white hover:bg-sky-600 disabled:cursor-not-allowed disabled:opacity-50">
      {checkout.isPending ? <LoaderCircle className="size-4 animate-spin" /> : null} {expired ? "Đã hết thời gian thanh toán" : `Thanh toán ${formatCurrency(Number(data.totalAmount))}`}
    </button>
  </div>;
}

function formatRemaining(seconds: number | null) {
  if (seconds === null) return "--:--";
  const minutes = Math.floor(seconds / 60);
  const rest = seconds % 60;
  return `${String(minutes).padStart(2, "0")}:${String(rest).padStart(2, "0")}`;
}

function Result({ icon, title, message, onDone }: { icon: React.ReactNode; title: string; message: string; onDone: () => void }) {
  return <div className="mx-auto max-w-xl rounded-[2rem] border border-slate-200 bg-white p-10 text-center shadow-sm">
    <span className="mx-auto grid size-12 place-items-center text-sky-600 [&_svg]:size-12">{icon}</span>
    <h1 className="mt-4 text-2xl font-black">{title}</h1><p className="mt-2 text-sm text-slate-500">{message}</p>
    <button onClick={onDone} className="action-button mt-6 bg-slate-950 px-5 text-white hover:bg-slate-800">Tiếp tục</button>
  </div>;
}
