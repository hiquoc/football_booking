"use client";

import { useMemo, useState } from "react";
import Link from "next/link";
import { CalendarDays, Clock3, Eye, MapPin, Phone, Save, UserRound } from "lucide-react";
import type { Booking, MatchResultOutcome, WinningTeam } from "@/lib/api/types";
import { bookingEndDateTime, bookingStartDateTime, formatBookingDateTime, getBookingStatus } from "@/lib/booking-format";
import { formatCurrency } from "@/lib/field-format";
import { useBookingDisplayStatus } from "@/lib/hooks/use-booking-display-status";
import { useReportNoShow, useSubmitMatchResult } from "@/lib/hooks/use-bookings";

const splitPresets = [
  { label: "50 / 50", a: 50, b: 50 },
  { label: "60 / 40", a: 60, b: 40 },
  { label: "70 / 30", a: 70, b: 30 },
  { label: "80 / 20", a: 80, b: 20 },
  { label: "90 / 10", a: 90, b: 10 },
  { label: "Tùy chỉnh", a: null, b: null },
] as const;

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
          <h2 className="mt-3 truncate text-xl font-black text-slate-950">{booking.fieldName}</h2>
          <p className="mt-1 flex items-center gap-2 text-sm font-semibold text-slate-500">
            <MapPin className="size-4 shrink-0 text-green-600" /> {booking.subFieldName}
          </p>
          {owner ? (
            <p className="mt-2 flex items-center gap-2 text-xs font-semibold text-slate-500">
              <UserRound className="size-3.5 text-slate-400" />
              Khách hàng: {customerName(booking)} {customerPhone(booking) ? (
                <>
                  <Phone className="size-4 text-slate-400" /> ` ${customerPhone(booking)}`
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

      <div className="mt-4 flex flex-wrap items-center justify-end gap-3">
        {action}
        {!owner ? (
          <Link
            href={`/bookings/${booking.id}`}
            className="inline-flex items-center gap-2 rounded-xl bg-green-600 px-4 py-2.5 text-sm font-black text-white hover:bg-green-700"
          >
            <Eye className="size-4" />
            Xem chi tiết
          </Link>
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
  const [teamAPercentage, setTeamAPercentage] = useState(result?.teamAPercentage ?? 70);
  const [teamBPercentage, setTeamBPercentage] = useState(result?.teamBPercentage ?? 30);
  const [matchResult, setMatchResult] = useState<MatchResultOutcome>(
    "BOOKER_WIN" as MatchResultOutcome,
  );
  const mutation = useSubmitMatchResult();
  const bookerName = customerName(booking);
  const noShowMutation = useReportNoShow();

  const teamAAmount = useMemo(
    () => (Number(booking.subFieldPrice ?? 0) * teamAPercentage) / 100,
    [booking.subFieldPrice, teamAPercentage],
  );
  const teamBAmount = useMemo(
    () => (Number(booking.subFieldPrice ?? 0) * teamBPercentage) / 100,
    [booking.subFieldPrice, teamBPercentage],
  );
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
          Đội A %
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
          Đội B %
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
          <p className="font-bold text-slate-500">Đội A trả</p>
          <p className="mt-1 text-lg font-black text-slate-950">{formatCurrency(teamAAmount)}</p>
        </div>
        <div className="rounded-lg bg-slate-50 p-3 text-sm">
          <p className="font-bold text-slate-500">Đội B trả</p>
          <p className="mt-1 text-lg font-black text-slate-950">{formatCurrency(teamBAmount)}</p>
        </div>
        <label className="grid gap-1 text-sm font-bold text-slate-600">
          Kết quả
          <select
            value={matchResult}
            onChange={(event) => setMatchResult(event.target.value as MatchResultOutcome)}
            className="rounded-lg border border-slate-200 px-3 py-2 text-sm"
          >
            <option value="BOOKER_WIN">{bookerTeamLabel(bookerName, "win")}</option>
            <option value="BOOKER_LOSS">{bookerTeamLabel(bookerName, "lose")}</option>
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
          Kết quả đã lưu: {winningTeamLabel(result.result ?? result.winningTeam, bookerName)} - {result.teamAPercentage}/{result.teamBPercentage}
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

        {booking.status === "COMPLETED" && !booking.matchResult ? (
          <button
            disabled={noShowMutation.isPending}
            onClick={() => {
              if (window.confirm("Xác nhận báo cáo khách hàng vắng mặt cho lịch đặt này?")) {
                noShowMutation.mutate(booking.id);
              }
            }}
            className="action-button min-h-0 rounded-lg bg-amber-500 px-3 py-2 text-xs text-white hover:bg-amber-600"
          >
            Báo vắng mặt
          </button>
        ) : null}
      </div>
      {mutation.error ? <p className="text-sm font-semibold text-rose-600">{mutation.error.message}</p> : null}

      {noShowMutation.error ? (
        <p className="rounded-lg border border-rose-200 bg-rose-50 px-4 py-3 text-sm font-semibold text-rose-700">
          {noShowMutation.error.message}
        </p>
      ) : null}
    </div>
  );
}

function matchingPreset(teamAPercentage: number, teamBPercentage: number) {
  return splitPresets.find((preset) => preset.a === teamAPercentage && preset.b === teamBPercentage)?.label ?? "Tùy chỉnh";
}

function winningTeamLabel(result: WinningTeam | undefined, bookerName: string) {
  const normalized = normalizeResult(result);
  if (normalized === "BOOKER_WIN") return bookerTeamLabel(bookerName, "win");
  if (normalized === "BOOKER_LOSS") return bookerTeamLabel(bookerName, "lose");
  return "Hòa";
}

function normalizeResult(result: WinningTeam | undefined): MatchResultOutcome {
  if (result === "TEAM_A") return "BOOKER_WIN";
  if (result === "TEAM_B") return "BOOKER_LOSS";
  return result ?? "DRAW";
}

function bookerTeamLabel(bookerName: string, outcome: "win" | "lose") {
  return `Đội của ${bookerName} ${outcome === "win" ? "thắng" : "thua"}`;
}

function customerName(booking: Booking) {
  return booking.clientName ? booking.clientName : "";
}

function customerPhone(booking: Booking) {
  return booking.clientPhoneNumber ? booking.clientPhoneNumber : "";
}
