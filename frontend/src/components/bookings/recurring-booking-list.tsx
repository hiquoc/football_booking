"use client";

import { useState } from "react";
import { CalendarClock, Pause, Pencil, Play, Save, Search, XCircle } from "lucide-react";
import type { RecurringBookingStatus } from "@/lib/api/types";
import {
  useRecurringBookingAction,
  useRecurringBookings,
  useUpdateRecurringBooking,
} from "@/lib/hooks/use-recurring-bookings";
import { DataEmpty, DataError, ListSkeleton } from "@/components/ui/data-state";

const statuses: Array<RecurringBookingStatus | "ALL"> = ["ALL", "ACTIVE", "PAUSED", "CANCELLED"];

export function RecurringBookingList({ scope }: { scope: "my" | "owner" | "admin" }) {
  const [page] = useState(0);
  const [status, setStatus] = useState<RecurringBookingStatus | "ALL">("ALL");
  const [editingId, setEditingId] = useState<string | null>(null);
  const bookings = useRecurringBookings(scope, page, 10, status === "ALL" ? undefined : status);
  const action = useRecurringBookingAction(scope === "admin");
  const update = useUpdateRecurringBooking();
  const readonly = scope === "owner";

  if (bookings.isPending) return <ListSkeleton count={4} />;
  if (bookings.isError) return <DataError title="Unable to load recurring bookings" />;
  if (!bookings.data.content.length) {
    return <DataEmpty title="No recurring bookings" description="Recurring weekly reservations will appear here." />;
  }

  return (
    <div className="space-y-5">
      <div className="flex flex-wrap items-center gap-2">
        <Search className="size-4 text-slate-400" />
        {statuses.map((item) => (
          <button
            key={item}
            type="button"
            onClick={() => setStatus(item)}
            className={`rounded-full border px-3 py-1.5 text-xs font-bold ${status === item ? "border-sky-500 bg-sky-50 text-sky-700" : "border-slate-200 text-slate-600"}`}
          >
            {item}
          </button>
        ))}
      </div>

      <div className="grid gap-4">
        {bookings.data.content.map((item) => (
          <article key={item.id} className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
            <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
              <div>
                <div className="flex items-center gap-2">
                  <CalendarClock className="size-5 text-sky-600" />
                  <h2 className="font-black text-slate-950">{item.fieldName ?? "Field"} - {item.subFieldName ?? "Sub-field"}</h2>
                </div>
                <p className="mt-2 text-sm text-slate-600">
                  {item.dayOfWeek} {item.startTime.slice(0, 5)}-{item.endTime.slice(0, 5)}
                  {" "}from {item.startDate} to {item.endDate}
                </p>
                <p className="mt-1 text-xs text-slate-500">Next execution: {new Date(item.nextProcessAt).toLocaleString()}</p>
              </div>
              <span className={`w-fit rounded-full px-3 py-1 text-xs font-black ${badgeClass(item.status)}`}>
                {item.status}
              </span>
            </div>

            {!readonly ? (
              <div className="mt-5 flex flex-wrap gap-2">
                <button type="button" onClick={() => setEditingId(editingId === item.id ? null : item.id)} className="action-button bg-slate-100 px-4 text-slate-700">
                  <Pencil className="size-4" /> Edit
                </button>
                {item.status === "ACTIVE" ? (
                  <button type="button" onClick={() => action.mutate({ id: item.id, action: "pause" })} className="action-button bg-slate-100 px-4 text-slate-700">
                    <Pause className="size-4" /> Pause
                  </button>
                ) : null}
                {item.status === "PAUSED" ? (
                  <button type="button" onClick={() => action.mutate({ id: item.id, action: "resume" })} className="action-button bg-emerald-500 px-4 text-white">
                    <Play className="size-4" /> Resume
                  </button>
                ) : null}
                {item.status !== "CANCELLED" ? (
                  <button
                    type="button"
                    onClick={() => window.confirm("Cancel this recurring booking?") && action.mutate({ id: item.id, action: "cancel" })}
                    className="action-button bg-rose-50 px-4 text-rose-700"
                  >
                    <XCircle className="size-4" /> Cancel
                  </button>
                ) : null}
              </div>
            ) : null}
            {editingId === item.id ? (
              <form
                className="mt-5 grid gap-3 rounded-2xl bg-slate-50 p-4 sm:grid-cols-5"
                onSubmit={(event) => {
                  event.preventDefault();
                  const form = new FormData(event.currentTarget);
                  update.mutate({
                    id: item.id,
                    input: {
                      subFieldId: item.subFieldId,
                      dayOfWeek: String(form.get("dayOfWeek")),
                      startTime: `${String(form.get("startTime"))}:00`,
                      endTime: `${String(form.get("endTime"))}:00`,
                      startDate: String(form.get("startDate")),
                      endDate: String(form.get("endDate")),
                    },
                  }, { onSuccess: () => setEditingId(null) });
                }}
              >
                <select name="dayOfWeek" defaultValue={item.dayOfWeek} className="input-field">
                  {["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"].map((day) => (
                    <option key={day} value={day}>{day}</option>
                  ))}
                </select>
                <input name="startTime" type="time" defaultValue={item.startTime.slice(0, 5)} className="input-field" />
                <input name="endTime" type="time" defaultValue={item.endTime.slice(0, 5)} className="input-field" />
                <input name="startDate" type="date" defaultValue={item.startDate} className="input-field" />
                <input name="endDate" type="date" defaultValue={item.endDate} className="input-field" />
                <button type="submit" className="action-button bg-sky-500 px-4 text-white sm:col-span-5">
                  <Save className="size-4" /> Save changes
                </button>
              </form>
            ) : null}
          </article>
        ))}
      </div>
      {action.error || update.error ? <p className="rounded-xl bg-rose-50 p-3 text-sm text-rose-700">{(action.error || update.error)?.message}</p> : null}
    </div>
  );
}

function badgeClass(status: RecurringBookingStatus) {
  if (status === "ACTIVE") return "bg-emerald-50 text-emerald-700";
  if (status === "PAUSED") return "bg-amber-50 text-amber-700";
  return "bg-slate-100 text-slate-600";
}
