"use client";

import { useMemo, useState } from "react";
import Link from "next/link";
import { CalendarDays, Clock3, MapPin, Save } from "lucide-react";
import type { Booking, WinningTeam } from "@/lib/api/types";
import { formatBookingDate, getBookingStatus } from "@/lib/booking-format";
import { formatCurrency, formatTime } from "@/lib/field-format";
import { useBookingDisplayStatus } from "@/lib/hooks/use-booking-display-status";
import { useSubmitMatchResult } from "@/lib/hooks/use-bookings";

const splitPresets = [
  { label: "50 / 50", a: 50, b: 50 },
  { label: "60 / 40", a: 60, b: 40 },
  { label: "70 / 30", a: 70, b: 30 },
  { label: "80 / 20", a: 80, b: 20 },
  { label: "90 / 10", a: 90, b: 10 },
  { label: "Custom", a: null, b: null },
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
  return (
    <article className="rounded-[1.5rem] border border-slate-200 bg-white p-5 shadow-sm transition hover:border-sky-200 hover:shadow-md">
      <div className="flex flex-col justify-between gap-4 sm:flex-row sm:items-start">
        <div>
          <div className="flex flex-wrap items-center gap-2">
            <span className={`rounded-full px-3 py-1 text-xs font-black ${status.className}`}>
              {status.label}
            </span>
            <span className="text-xs font-bold text-slate-400">{booking.bookingCode}</span>
            <span className="text-xs font-bold text-slate-400">ID: {booking.id}</span>
          </div>
          <h2 className="mt-3 text-xl font-black text-slate-700">{booking.fieldName}</h2>
          <p className="mt-1 flex items-center gap-2 text-sm text-slate-500">
            <MapPin className="size-4 text-sky-600" /> {booking.subFieldName}
          </p>
          {owner ? (
            <p className="mt-2 text-xs font-semibold text-slate-500">Customer: {booking.clientId}</p>
          ) : null}
        </div>
        <strong className="text-lg text-slate-950">
          {formatCurrency(Number(booking.totalAmount))}
        </strong>
      </div>
      <div className="mt-5 flex flex-wrap gap-x-6 gap-y-2 border-t border-slate-100 pt-4 text-sm text-slate-500">
        <span className="inline-flex items-center gap-2">
          <CalendarDays className="size-4" /> {formatBookingDate(booking.bookingDate)}
        </span>
        <span className="inline-flex items-center gap-2">
          <Clock3 className="size-4" /> {formatTime(booking.startTime)} - {formatTime(booking.endTime)}
        </span>
        <span>Total amount: {formatCurrency(Number(booking.totalAmount))}</span>
        <span className="ml-auto flex items-center gap-3">
          {action}
          {!owner ? (
            <Link
              href={`/bookings/${booking.id}`}
              className="rounded-lg px-2 py-1 font-black text-sky-600 hover:bg-sky-50"
            >
              View details
            </Link>
          ) : null}
        </span>
      </div>
      {owner && booking.status === "COMPLETED" ? (
        <MatchResultEditor key={booking.matchResult?.updatedAt ?? "new-result"} booking={booking} />
      ) : null}
    </article>
  );
}

function MatchResultEditor({ booking }: { booking: Booking }) {
  const result = booking.matchResult;
  const [teamAPercentage, setTeamAPercentage] = useState(result?.teamAPercentage ?? 50);
  const [teamBPercentage, setTeamBPercentage] = useState(result?.teamBPercentage ?? 50);
  const [winningTeam, setWinningTeam] = useState<WinningTeam>(result?.winningTeam ?? "DRAW");
  const mutation = useSubmitMatchResult();

  const teamAAmount = useMemo(
    () => (Number(booking.totalAmount) * teamAPercentage) / 100,
    [booking.totalAmount, teamAPercentage],
  );
  const teamBAmount = useMemo(
    () => (Number(booking.totalAmount) * teamBPercentage) / 100,
    [booking.totalAmount, teamBPercentage],
  );
  const splitValid = teamAPercentage + teamBPercentage === 100;

  return (
    <div className="mt-5 grid gap-4 border-t border-slate-100 pt-4">
      <div className="grid gap-3 md:grid-cols-3">
        <label className="grid gap-1 text-sm font-bold text-slate-600">
          Split preset
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
          Team A %
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
          Team B %
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
          <p className="font-bold text-slate-500">Team A pays</p>
          <p className="mt-1 text-lg font-black text-slate-950">{formatCurrency(teamAAmount)}</p>
        </div>
        <div className="rounded-lg bg-slate-50 p-3 text-sm">
          <p className="font-bold text-slate-500">Team B pays</p>
          <p className="mt-1 text-lg font-black text-slate-950">{formatCurrency(teamBAmount)}</p>
        </div>
        <label className="grid gap-1 text-sm font-bold text-slate-600">
          Result
          <select
            value={winningTeam}
            onChange={(event) => setWinningTeam(event.target.value as WinningTeam)}
            className="rounded-lg border border-slate-200 px-3 py-2 text-sm"
          >
            <option value="TEAM_A">Team A won</option>
            <option value="TEAM_B">Team B won</option>
            <option value="DRAW">Draw</option>
          </select>
        </label>
      </div>
      {!splitValid ? (
        <p className="rounded-lg bg-rose-50 p-3 text-sm font-semibold text-rose-700">
          Team A and Team B percentages must total 100%.
        </p>
      ) : null}
      {result ? (
        <p className="text-sm font-semibold text-slate-500">
          Saved result: {result.winningTeam.replace("_", " ")} - {result.teamAPercentage}/{result.teamBPercentage}
        </p>
      ) : null}
      <button
        disabled={mutation.isPending || !splitValid}
        onClick={() =>
          mutation.mutate({
            bookingId: booking.id,
            input: { winningTeam, teamAPercentage, teamBPercentage },
          })
        }
        className="inline-flex w-fit items-center gap-2 rounded-lg bg-slate-950 px-4 py-2 text-sm font-bold text-white disabled:opacity-60"
      >
        <Save className="size-4" />
        {result ? "Update result" : "Submit result"}
      </button>
      {mutation.error ? <p className="text-sm font-semibold text-rose-600">{mutation.error.message}</p> : null}
    </div>
  );
}

function matchingPreset(teamAPercentage: number, teamBPercentage: number) {
  return splitPresets.find((preset) => preset.a === teamAPercentage && preset.b === teamBPercentage)?.label ?? "Custom";
}
