"use client";

import { type FormEvent, useState } from "react";
import Link from "next/link";
import { AlertTriangle, CalendarDays, Clock3, Eye, MapPin, RotateCcw, Save, UserRound, X } from "lucide-react";
import type { Booking, MatchResultOutcome, WinningTeam } from "@/lib/api/types";
import { bookingEndDateTime, bookingStartDateTime, formatBookingDateTime, getBookingStatus } from "@/lib/booking-format";
import { formatCurrency } from "@/lib/field-format";
import { useBookingDisplayStatus } from "@/lib/hooks/use-booking-display-status";
import { useCreatePaymentDispute, useReportNoShow, useSubmitMatchResult } from "@/lib/hooks/use-bookings";
import { RecurringPaymentDeadline } from "./recurring-payment-deadline";

const splitPresets = [
  { label: "50 / 50", a: 50, b: 50 },
  { label: "60 / 40", a: 40, b: 60 },
  { label: "70 / 30", a: 30, b: 70 },
  { label: "80 / 20", a: 20, b: 80 },
  { label: "90 / 10", a: 10, b: 90 },
  { label: "Tùy chỉnh", a: null, b: null },
] as const;

const today = new Date();

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
  const fieldPrice = Number(booking.subFieldPrice ?? 0);
  const bookingFee = Number(booking.bookingPrice ?? booking.platformBookingFee ?? 0);
  return (
    <article className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm transition hover:border-green-300 hover:shadow-[0_16px_36px_rgba(15,23,42,0.08)] sm:p-6">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
        <div className="min-w-0">
          <div className="flex flex-wrap items-center gap-2">
            <span className={`rounded-full px-3 py-1 text-xs font-black ${status.className}`}>
              {status.label}
            </span>
            <span className="text-xs font-semibold text-slate-400">{booking.bookingCode}</span>
          </div>
          <h2 className="mt-3 truncate text-xl font-black text-green-700">
            <FieldNameLink booking={booking} />
          </h2>
          <p className="mt-1 flex items-center gap-2 text-sm font-semibold text-slate-500">
            <MapPin className="size-4 shrink-0 text-green-600" /> {booking.subFieldName}
          </p>
          {owner ? (
            <p className="mt-2 flex items-center gap-2 text-xs font-semibold text-slate-500">
              <UserRound className="size-3.5 text-slate-400" />
              Khách hàng: {customerName(booking)} {customerPhone(booking) ? (
                <>
                  ({`${customerPhone(booking)}`})
                </>) : ""}
            </p>
          ) : null}
        </div>

        <div className="grid shrink-0 gap-2 text-sm sm:min-w-56">
          <PriceItem label="Thanh toán tại sân" value={fieldPrice} />
          <PriceItem label="Phí đặt lịch" value={bookingFee} strong />
        </div>
      </div>

      <div className="mt-4 grid gap-x-6 gap-y-3 border-t border-slate-100 pt-4 text-sm text-slate-600 sm:grid-cols-3">
        <BookingMeta
          icon={<CalendarDays />}
          label="Bắt đầu"
          value={formatBookingDateTime(bookingStartDateTime(booking))}
        />
        <BookingMeta
          icon={<Clock3 />}
          label="Kết thúc"
          value={formatBookingDateTime(bookingEndDateTime(booking))}
        />
        <BookingMeta
          icon={<Clock3 />}
          label="Tạo lúc"
          value={formatBookingDateTime(booking.createdAt)}
        />
      </div>

      {!owner ? (
        <div className="mt-4">
          <RecurringPaymentDeadline booking={booking} compact />
        </div>
      ) : null}

      <div className="mt-4 flex flex-wrap items-center justify-end gap-3">
        {action}
        {!owner ? (
          <>
            {booking.status === "COMPLETED" && booking.fieldId ? (
              <Link
                href={rebookHref(booking)}
                className="inline-flex items-center gap-2 rounded-xl border border-green-200 bg-white px-4 py-2.5 text-sm font-black text-green-700 hover:border-green-300 hover:bg-green-50"
              >
                <RotateCcw className="size-4" />
                Đặt lại
              </Link>
            ) : null}
            <Link
              href={`/bookings/${booking.id}`}
              className="inline-flex items-center gap-2 rounded-xl bg-green-600 px-4 py-2.5 text-sm font-black text-white hover:bg-green-700"
            >
              <Eye className="size-4" />
              Xem chi tiết
            </Link>
          </>
        ) : null}
      </div>
      {owner && booking.status === "COMPLETED" ? (
        <MatchResultEditor key={booking.matchResult?.updatedAt ?? "new-result"} booking={booking} />
      ) : null}
    </article>
  );
}

function PriceItem({ label, value, strong = false }: { label: string; value: number; strong?: boolean }) {
  return (
    <div className="flex items-center justify-between gap-5 sm:justify-end">
      <span className="text-slate-500">{label}</span>
      <strong className={strong ? "text-slate-950" : "font-semibold text-slate-700"}>{formatCurrency(value)}</strong>
    </div>
  );
}

function BookingMeta({
  icon,
  label,
  value,
}: {
  icon: React.ReactNode;
  label: string;
  value: string;
}) {
  return (
    <div className="flex min-w-0 items-start gap-2">
      <span className="mt-0.5 shrink-0 text-green-600 [&_svg]:size-4">{icon}</span>
      <div className="min-w-0">
        <p className="text-xs font-bold text-slate-400">{label}</p>
        <p className="mt-0.5 font-semibold text-slate-700">{value}</p>
      </div>
    </div>
  );
}

function MatchResultEditor({ booking }: { booking: Booking }) {
  const result = booking.matchResult;
  const [teamAPercentage, setTeamAPercentage] = useState(result?.teamAPercentage ?? 30);
  const [teamBPercentage, setTeamBPercentage] = useState(result?.teamBPercentage ?? 70);
  const [matchResult, setMatchResult] = useState<MatchResultOutcome>(
    "BOOKER_WIN" as MatchResultOutcome,
  );
  const mutation = useSubmitMatchResult();
  const bookerName = customerName(booking);
  const bookerTeamName = teamName(bookerName);
  const opponentTeamName = teamName(booking.opponentName || "khách");
  const [reportOpen, setReportOpen] = useState(false);
  const noShowMutation = useReportNoShow();
  const paymentDisputeMutation = useCreatePaymentDispute();

  const subFieldPrice = Number(booking.subFieldPrice ?? 0);

  const teamBAmount =
    Math.round((subFieldPrice * teamBPercentage) / 100 / 5000) * 5000;

  const teamAAmount = subFieldPrice - teamBAmount;

  const splitValid = teamAPercentage + teamBPercentage === 100;

  return (
    <div className="mt-5 grid gap-4 border-t border-slate-100 pt-4">
      <div className="grid gap-3 md:grid-cols-3">
        <label className="grid gap-1 text-sm font-bold text-slate-600">
          Tỷ lệ chia tiền
          <select
            className="rounded-lg border border-slate-200 px-3 py-2 text-sm"
            value={matchingPreset(teamAPercentage, teamBPercentage)}
            onChange={(event) => {
              const preset = splitPresets.find((item) => item.label === event.target.value);
              if (preset?.a != null && preset.b != null) {
                setTeamAPercentage(preset.a);
                setTeamBPercentage(preset.b);
              }
            }}
          >
            {splitPresets.map((preset) => (
              <option key={preset.label} value={preset.label}>{preset.label}</option>
            ))}
          </select>
        </label>
        <label className="grid gap-1 text-sm font-bold text-slate-600">
          {bookerTeamName} %
          <input
            type="number"
            min={0}
            max={100}
            value={teamAPercentage}
            onChange={(event) => setTeamAPercentage(Number(event.target.value))}
            className="rounded-lg border border-slate-200 px-3 py-2 text-sm"
          />
        </label>
        <label className="grid gap-1 text-sm font-bold text-slate-600">
          {opponentTeamName} %
          <input
            type="number"
            min={0}
            max={100}
            value={teamBPercentage}
            onChange={(event) => setTeamBPercentage(Number(event.target.value))}
            className="rounded-lg border border-slate-200 px-3 py-2 text-sm"
          />
        </label>
      </div>
      <div className="grid gap-3 md:grid-cols-3">
        <div className="rounded-lg bg-slate-50 p-3 text-sm">
          <TeamProfileLabel userId={booking.clientId} label={bookerTeamName} />
          <p className="mt-1 text-lg font-black text-slate-950">{formatCurrency(teamAAmount)}</p>
        </div>
        <div className="rounded-lg bg-slate-50 p-3 text-sm">
          <TeamProfileLabel userId={booking.opponentId} label={opponentTeamName} />
          <p className="mt-1 text-lg font-black text-slate-950">{formatCurrency(teamBAmount)}</p>
        </div>
        <label className="grid gap-1 text-sm font-bold text-slate-600">
          Kết quả
          <select
            value={matchResult}
            onChange={(event) => setMatchResult(event.target.value as MatchResultOutcome)}
            className="rounded-lg border border-slate-200 px-3 py-2 text-sm"
          >
            <option value="BOOKER_WIN">{resultOptionLabel(bookerTeamName)}</option>
            <option value="BOOKER_LOSS">{resultOptionLabel(opponentTeamName)}</option>
            <option value="DRAW">Hòa</option>
          </select>
        </label>
      </div>
      {!splitValid ? (
        <p className="rounded-lg bg-rose-50 p-3 text-sm font-semibold text-rose-700">
          Tổng tỷ lệ của đội A và đội B phải bằng 100%.
        </p>
      ) : null}
      {result ? (
        <p className="text-sm font-semibold text-slate-500">
          Kết quả đã lưu: {winningTeamLabel(result.result ?? result.winningTeam, bookerTeamName, opponentTeamName)} - {result.teamAPercentage}/{result.teamBPercentage}
        </p>
      ) : null}
      <div className="flex justify-between">
        <button
          disabled={mutation.isPending || !splitValid}
          onClick={() =>
            mutation.mutate({
              bookingId: booking.id,
              input: { result: matchResult, teamAPercentage, teamBPercentage },
            })
          }
          className="inline-flex w-fit items-center gap-2 rounded-xl bg-green-600 px-4 py-2 text-sm font-bold text-white hover:bg-green-700 disabled:opacity-60"
        >
          <Save className="size-4" />
          {result ? "Cập nhật kết quả" : "Lưu kết quả"}
        </button>

        {booking.status === "REPORTED" || (booking.status === "COMPLETED" && !booking.matchResult) ? (
          <button
            disabled={noShowMutation.isPending || paymentDisputeMutation.isPending}
            onClick={() => setReportOpen(true)}
            className="action-button min-h-0 rounded-lg bg-amber-500 px-3 py-2 text-xs text-white hover:bg-amber-600"
          >
            Báo cáo
          </button>
        ) : null}
      </div>
      {mutation.error ? <p className="text-sm font-semibold text-rose-600">{mutation.error.message}</p> : null}

      {(noShowMutation.error || paymentDisputeMutation.error) ? (
        <p className="rounded-lg border border-rose-200 bg-rose-50 px-4 py-3 text-sm font-semibold text-rose-700">
          {(noShowMutation.error ?? paymentDisputeMutation.error)?.message}
        </p>
      ) : null}
      {reportOpen ? (
        <BookingReportDialog
          booking={booking}
          noShowPending={noShowMutation.isPending}
          paymentDisputePending={paymentDisputeMutation.isPending}
          onClose={() => setReportOpen(false)}
          onReportNoShow={() =>
            noShowMutation.mutate(booking.id, {
              onSuccess: () => setReportOpen(false),
            })
          }
          onReportPaymentDispute={(description) =>
            paymentDisputeMutation.mutate(
              { bookingId: booking.id, description },
              { onSuccess: () => setReportOpen(false) },
            )
          }
        />
      ) : null}
    </div>
  );
}

type ReportReason = "NO_SHOW" | "UNPAID_FIELD";

function BookingReportDialog({
  booking,
  noShowPending,
  paymentDisputePending,
  onClose,
  onReportNoShow,
  onReportPaymentDispute,
}: {
  booking: Booking;
  noShowPending: boolean;
  paymentDisputePending: boolean;
  onClose: () => void;
  onReportNoShow: () => void;
  onReportPaymentDispute: (description: string) => void;
}) {
  const [reason, setReason] = useState<ReportReason>("NO_SHOW");
  const [description, setDescription] = useState(
    `Khách hàng ${customerName(booking) || "này"} không trả tiền sân cho lịch ${booking.bookingCode}.`,
  );
  const [validationError, setValidationError] = useState("");
  const pending = noShowPending || paymentDisputePending;

  function submitReport(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setValidationError("");

    if (reason === "NO_SHOW") {
      onReportNoShow();
      return;
    }

    if (!description.trim()) {
      setValidationError("Nhập mô tả cho báo cáo.");
      return;
    }
    onReportPaymentDispute(description.trim());
  }

  return (
    <div
      className="fixed inset-0 z-[90] grid place-items-center bg-slate-950/55 p-4"
      role="dialog"
      aria-modal="true"
      aria-labelledby={`report-booking-${booking.id}`}
    >
      <form
        onSubmit={submitReport}
        className="w-full max-w-lg rounded-2xl border border-slate-200 bg-white p-5 shadow-2xl shadow-slate-950/20"
      >
        <div className="flex items-start justify-between gap-4">
          <div>
            <p className="inline-flex items-center gap-2 text-xs font-black uppercase text-amber-600">
              <AlertTriangle className="size-4" />
              Báo cáo lịch đặt
            </p>
            <h3 id={`report-booking-${booking.id}`} className="mt-2 text-lg font-black text-slate-950">
              Báo cáo lịch {booking.bookingCode}
            </h3>
            <p className="mt-1 text-sm font-semibold text-slate-500">
              Khách hàng: {customerName(booking) || "Khách hàng"}
            </p>
          </div>
          <button
            type="button"
            onClick={onClose}
            disabled={pending}
            className="rounded-full p-2 text-slate-500 hover:bg-slate-100 hover:text-slate-900 disabled:opacity-50"
            aria-label="Đóng"
          >
            <X className="size-5" />
          </button>
        </div>

        <div className="mt-5 grid gap-3">
          <label className={`flex cursor-pointer gap-3 rounded-xl border p-4 ${reason === "NO_SHOW" ? "border-amber-300 bg-amber-50" : "border-slate-200 bg-white"}`}>
            <input
              type="radio"
              name="reportReason"
              value="NO_SHOW"
              checked={reason === "NO_SHOW"}
              onChange={() => setReason("NO_SHOW")}
              className="mt-1"
            />
            <span>
              <span className="block text-sm font-black text-slate-950">Vắng mặt</span>
              <span className="mt-1 block text-sm font-semibold text-slate-500">
                Ghi nhận khách không đến sân và cập nhật lịch sử vi phạm.
              </span>
            </span>
          </label>

          <label className={`flex cursor-pointer gap-3 rounded-xl border p-4 ${reason === "UNPAID_FIELD" ? "border-amber-300 bg-amber-50" : "border-slate-200 bg-white"}`}>
            <input
              type="radio"
              name="reportReason"
              value="UNPAID_FIELD"
              checked={reason === "UNPAID_FIELD"}
              onChange={() => setReason("UNPAID_FIELD")}
              className="mt-1"
            />
            <span>
              <span className="block text-sm font-black text-slate-950">Không trả tiền sân</span>
              <span className="mt-1 block text-sm font-semibold text-slate-500">
                Gửi tranh chấp thanh toán cho quản trị viên xem xét.
              </span>
            </span>
          </label>
        </div>

        {reason === "UNPAID_FIELD" ? (
          <div className="mt-4 grid gap-3">
            <label className="grid gap-1 text-sm font-bold text-slate-600">
              Mô tả
              <textarea
                value={description}
                onChange={(event) => setDescription(event.target.value)}
                rows={3}
                className="resize-none rounded-xl border border-slate-200 px-3 py-2 text-sm font-semibold outline-none focus:border-amber-400 focus:ring-4 focus:ring-amber-100"
              />
            </label>
          </div>
        ) : (
          <p className="mt-4 rounded-xl border border-amber-200 bg-amber-50 px-4 py-3 text-sm font-semibold text-amber-800">
            Xác nhận chỉ khi khách hàng thực sự không đến sân cho lịch này.
          </p>
        )}

        {validationError ? (
          <p className="mt-3 rounded-lg border border-rose-200 bg-rose-50 px-3 py-2 text-sm font-semibold text-rose-700">
            {validationError}
          </p>
        ) : null}

        <div className="mt-5 flex flex-col-reverse gap-3 sm:flex-row sm:justify-end">
          <button
            type="button"
            onClick={onClose}
            disabled={pending}
            className="rounded-xl border border-slate-200 px-4 py-2.5 text-sm font-black text-slate-700 hover:bg-slate-50 disabled:opacity-50"
          >
            Hủy
          </button>
          <button
            type="submit"
            disabled={pending}
            className="rounded-xl bg-amber-500 px-4 py-2.5 text-sm font-black text-white hover:bg-amber-600 disabled:opacity-60"
          >
            {pending ? "Đang gửi..." : "Xác nhận báo cáo"}
          </button>
        </div>
      </form>
    </div>
  );
}

function FieldNameLink({ booking }: { booking: Booking }) {
  if (!booking.fieldId) {
    return <>{booking.fieldName}</>;
  }
  return (
    <Link href={`/fields/${booking.fieldId}`} className="hover:text-green-800">
      {booking.fieldName}
    </Link>
  );
}

function rebookHref(booking: Booking) {
  if (!booking.fieldId) return "/fields";
  const params = new URLSearchParams();
  const currentDate = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, "0")}-${String(today.getDate()).padStart(2, "0")}`;

  params.set("subFieldId", booking.subFieldId);
  params.set("duration", String(booking.durationMinutes));
  params.set("slot", `${currentDate}T${booking.startTime.slice(0, 5)}`);
  return `/fields/${booking.fieldId}/book?${params}`;
}

function TeamProfileLabel({ userId, label }: { userId?: string | null; label: string }) {
  const content = `${label} trả`;
  if (!userId) {
    return <p className="font-bold text-slate-500">{content}</p>;
  }
  return (
    <Link href={`/users/${encodeURIComponent(userId)}/profile`} className="font-bold text-green-700 hover:text-green-800">
      {content}
    </Link>
  );
}

function matchingPreset(teamAPercentage: number, teamBPercentage: number) {
  return splitPresets.find((preset) => preset.a === teamAPercentage && preset.b === teamBPercentage)?.label ?? "Tùy chỉnh";
}

function winningTeamLabel(result: WinningTeam | undefined, bookerTeamName: string, opponentTeamName: string) {
  const normalized = normalizeResult(result);
  if (normalized === "BOOKER_WIN") return resultOptionLabel(bookerTeamName);
  if (normalized === "BOOKER_LOSS") return resultOptionLabel(opponentTeamName);
  return "Hòa";
}

function normalizeResult(result: WinningTeam | undefined): MatchResultOutcome {
  if (result === "TEAM_A") return "BOOKER_WIN";
  if (result === "TEAM_B") return "BOOKER_LOSS";
  return result ?? "DRAW";
}

function resultOptionLabel(teamLabel: string) {
  return `${teamLabel} thắng`;
}

function teamName(name: string) {
  return `Đội ${name || "khách"}`;
}

function customerName(booking: Booking) {
  return booking.clientName ? booking.clientName : "";
}

function customerPhone(booking: Booking) {
  return booking.clientPhoneNumber ? booking.clientPhoneNumber : "";
}
