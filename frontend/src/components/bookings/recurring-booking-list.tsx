"use client";

import { useState } from "react";
import { CalendarClock, Pause, Pencil, Play, Save, Search, XCircle } from "lucide-react";
import type { RecurringBookingStatus } from "@/lib/api/types";
import { formatCurrency } from "@/lib/field-format";
import { useCancelBooking } from "@/lib/hooks/use-bookings";
import {
  useRecurringBookingAction,
  useRecurringBookings,
  useUpdateRecurringBooking,
} from "@/lib/hooks/use-recurring-bookings";
import { DataEmpty, DataError, ListSkeleton } from "@/components/ui/data-state";

const statuses: Array<RecurringBookingStatus | "ALL"> = ["ALL", "ACTIVE", "PAUSED", "CANCELLED"];

type ConfirmAction =
  | { id: string; action: "pause" | "cancel" }
  | { id: string; action: "abort"; bookingId: string }
  | null;

export function RecurringBookingList({ scope }: { scope: "my" | "owner" | "admin" }) {
  const [page] = useState(0);
  const [status, setStatus] = useState<RecurringBookingStatus | "ALL">("ALL");
  const [editingId, setEditingId] = useState<string | null>(null);
  const [confirmAction, setConfirmAction] = useState<ConfirmAction>(null);
  const bookings = useRecurringBookings(scope, page, 10, status === "ALL" ? undefined : status);
  const action = useRecurringBookingAction(scope === "admin");
  const update = useUpdateRecurringBooking();
  const abortBooking = useCancelBooking(false);
  const readonly = scope === "owner";

  if (bookings.isPending) return <ListSkeleton count={4} />;
  if (bookings.isError) return <DataError title="Unable to load recurring bookings" />;
  if (!bookings.data.content.length) {
    return <DataEmpty title="No recurring bookings" description="Recurring reservations will appear here." />;
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
        {bookings.data.content.map((item) => {
          const latestBooking = item.latestBooking;
          const showAbortLatest = item.status === "PAUSED" && latestBooking?.status === "CONFIRMED";
          return (
            <article key={item.id} className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
              <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
                <div>
                  <div className="flex items-center gap-2">
                    <CalendarClock className="size-5 text-sky-600" />
                    <h2 className="font-black text-slate-950">{item.fieldName ?? "Field"} - {item.subFieldName ?? "Sub-field"}</h2>
                  </div>
                  <p className="mt-2 text-sm text-slate-600">
                    Every {item.intervalDays} day(s) {item.startTime.slice(0, 5)}-{item.endTime.slice(0, 5)}
                    {" "}from {item.startDate} to {item.endDate}
                  </p>
                  <p className="mt-1 text-xs text-slate-500">Next execution: {new Date(item.nextProcessAt).toLocaleString()}</p>
                </div>
                <span className={`w-fit rounded-full px-3 py-1 text-xs font-black ${badgeClass(item.status)}`}>
                  {item.status}
                </span>
              </div>

              {showAbortLatest ? (
                <div className="mt-4 rounded-xl border border-amber-200 bg-amber-50 p-4 text-sm text-amber-900">
                  <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
                    <div>
                      <p className="font-bold">Latest confirmed booking</p>
                      <p className="mt-1 text-xs">
                        {latestBooking.bookingDate} {latestBooking.startTime.slice(0, 5)}-{latestBooking.endTime.slice(0, 5)}
                        {" "}· {formatCurrency(Number(latestBooking.bookingPrice ?? latestBooking.platformBookingFee ?? 0))}
                      </p>
                    </div>
                    {confirmAction?.id === item.id && confirmAction.action === "abort" ? (
                      <button
                        type="button"
                        disabled={abortBooking.isPending}
                        onClick={() => abortBooking.mutate({
                          id: confirmAction.bookingId,
                          reason: "Recurring booking paused by client",
                        }, { onSuccess: () => setConfirmAction(null) })}
                        className="action-button bg-rose-500 px-4 text-white"
                      >
                        Confirm abort
                      </button>
                    ) : (
                      <button
                        type="button"
                        onClick={() => setConfirmAction({ id: item.id, action: "abort", bookingId: latestBooking.id })}
                        className="action-button bg-white px-4 text-rose-700"
                      >
                        Abort latest booking
                      </button>
                    )}
                  </div>
                </div>
              ) : null}

              {!readonly ? (
                <div className="mt-5 flex flex-wrap gap-2">
                  <button type="button" onClick={() => setEditingId(editingId === item.id ? null : item.id)} className="action-button bg-slate-100 px-4 text-slate-700">
                    <Pencil className="size-4" /> Edit end date
                  </button>
                  {item.status === "ACTIVE" ? (
                    <ConfirmableAction
                      id={item.id}
                      actionName="pause"
                      confirmAction={confirmAction}
                      disabled={action.isPending}
                      pendingLabel="Confirm pause"
                      label="Pause"
                      className="action-button bg-slate-100 px-4 text-slate-700"
                      onAsk={() => setConfirmAction({ id: item.id, action: "pause" })}
                      onConfirm={() => action.mutate({ id: item.id, action: "pause" }, { onSuccess: () => setConfirmAction(null) })}
                      icon={<Pause className="size-4" />}
                    />
                  ) : null}
                  {item.status === "PAUSED" ? (
                    <button type="button" onClick={() => action.mutate({ id: item.id, action: "resume" })} className="action-button bg-emerald-500 px-4 text-white">
                      <Play className="size-4" /> Resume
                    </button>
                  ) : null}
                  {item.status !== "CANCELLED" ? (
                    <ConfirmableAction
                      id={item.id}
                      actionName="cancel"
                      confirmAction={confirmAction}
                      disabled={action.isPending}
                      pendingLabel="Confirm cancel"
                      label="Cancel"
                      className="action-button bg-rose-50 px-4 text-rose-700"
                      onAsk={() => setConfirmAction({ id: item.id, action: "cancel" })}
                      onConfirm={() => action.mutate({ id: item.id, action: "cancel" }, { onSuccess: () => setConfirmAction(null) })}
                      icon={<XCircle className="size-4" />}
                    />
                  ) : null}
                </div>
              ) : null}

              {editingId === item.id ? (
                <form
                  className="mt-5 grid gap-3 rounded-2xl bg-slate-50 p-4 sm:grid-cols-[1fr_auto]"
                  onSubmit={(event) => {
                    event.preventDefault();
                    const form = new FormData(event.currentTarget);
                    update.mutate({
                      id: item.id,
                      input: {
                        subFieldId: item.subFieldId,
                        startTime: item.startTime,
                        endTime: item.endTime,
                        startDate: item.startDate,
                        endDate: String(form.get("endDate")),
                        intervalDays: item.intervalDays,
                      },
                    }, { onSuccess: () => setEditingId(null) });
                  }}
                >
                  <input name="endDate" type="date" min={item.startDate} defaultValue={item.endDate} className="input-field" />
                  <button type="submit" className="action-button bg-sky-500 px-4 text-white">
                    <Save className="size-4" /> Save end date
                  </button>
                </form>
              ) : null}
            </article>
          );
        })}
      </div>
      {action.error || update.error || abortBooking.error ? (
        <p className="rounded-xl bg-rose-50 p-3 text-sm text-rose-700">
          {(action.error || update.error || abortBooking.error)?.message}
        </p>
      ) : null}
    </div>
  );
}

function ConfirmableAction({
  id,
  actionName,
  confirmAction,
  disabled,
  pendingLabel,
  label,
  className,
  onAsk,
  onConfirm,
  icon,
}: {
  id: string;
  actionName: "pause" | "cancel";
  confirmAction: ConfirmAction;
  disabled: boolean;
  pendingLabel: string;
  label: string;
  className: string;
  onAsk: () => void;
  onConfirm: () => void;
  icon: React.ReactNode;
}) {
  const confirming = confirmAction?.id === id && confirmAction.action === actionName;
  return (
    <button
      type="button"
      disabled={disabled}
      onClick={confirming ? onConfirm : onAsk}
      className={confirming ? "action-button bg-slate-950 px-4 text-white" : className}
    >
      {icon} {confirming ? pendingLabel : label}
    </button>
  );
}

function badgeClass(status: RecurringBookingStatus) {
  if (status === "ACTIVE") return "bg-emerald-50 text-emerald-700";
  if (status === "PAUSED") return "bg-amber-50 text-amber-700";
  return "bg-slate-100 text-slate-600";
}
