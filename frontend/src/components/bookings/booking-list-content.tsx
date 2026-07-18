"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useTransition, type FormEvent } from "react";
import type { BookingStatus } from "@/lib/api/types";
import { useBookingList, useCancelBooking } from "@/lib/hooks/use-bookings";
import { BookingCard } from "./booking-card";
import { DataEmpty, DataError, ListSkeleton } from "@/components/ui/data-state";

export function BookingListContent({
  page,
  owner = false,
  filters = {},
}: {
  page: number;
  owner?: boolean;
  filters?: { bookingDate?: string; subFieldId?: string; status?: string };
}) {
  const query = useBookingList(page, owner, 10, filters);
  const cancelMutation = useCancelBooking(true);

  if (query.isPending) {
    return owner ? (
      <>
        <OwnerFilters filters={filters} />
        <ListSkeleton />
      </>
    ) : (
      <ListSkeleton />
    );
  }
  if (query.isError) return <DataError title="Cannot load bookings" />;
  if (!query.data.content.length) {
    return (
      <>
        {owner ? <OwnerFilters filters={filters} /> : null}
        <DataEmpty
          title="No bookings yet"
          description={
            owner
              ? "Customer bookings will appear here."
              : "Choose a suitable field and start your first match."
          }
        />
      </>
    );
  }

  return (
    <div className="space-y-4">
      {owner ? <OwnerFilters filters={filters} /> : null}
      {query.data.content.map((booking) => (
        <BookingCard
          key={booking.id}
          booking={booking}
          owner={owner}
          action={
            owner &&
            (booking.status === "PENDING" || booking.status === "CONFIRMED") ? (
              <button
                disabled={cancelMutation.isPending}
                onClick={() => {
                  if (window.confirm("Confirm cancelling this booking?")) {
                    cancelMutation.mutate({
                      id: booking.id,
                      reason: "Owner cancelled booking",
                    });
                  }
                }}
                className="action-button min-h-0 rounded-lg bg-rose-500 px-3 py-2 text-xs text-white hover:bg-rose-600"
              >
                Cancel
              </button>
            ) : undefined
          }
        />
      ))}
      {query.data.totalPages > 1 ? (
        <div className="flex justify-center gap-3 pt-5">
          {page > 0 ? (
            <Link
              className="rounded-full border border-slate-200 bg-white px-4 py-2 text-sm font-bold"
              href={pageLink(page, filters)}
            >
              Previous
            </Link>
          ) : null}
          {page + 1 < query.data.totalPages ? (
            <Link
              className="rounded-full bg-slate-950 px-4 py-2 text-sm font-bold text-white"
              href={pageLink(page + 2, filters)}
            >
              Next
            </Link>
          ) : null}
        </div>
      ) : null}
    </div>
  );
}

function OwnerFilters({
  filters,
}: {
  filters: { bookingDate?: string; subFieldId?: string; status?: string };
}) {
  const router = useRouter();
  const [pending, startTransition] = useTransition();

  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    const params = new URLSearchParams();
    ["bookingDate", "subFieldId", "status"].forEach((key) => {
      const value = String(form.get(key) ?? "").trim();
      if (value) params.set(key, value);
    });
    startTransition(() => router.push(`/owner/bookings${params.size ? `?${params}` : ""}`));
  }

  return (
    <form
      onSubmit={submit}
      className="grid gap-3 rounded-lg border border-slate-200 bg-white p-4 shadow-sm md:grid-cols-[1fr_1fr_1fr_auto_auto]"
    >
      <input
        name="bookingDate"
        type="date"
        defaultValue={filters.bookingDate ?? ""}
        className="rounded-lg border border-slate-200 px-3 py-2 text-sm font-medium"
      />
      <input
        name="subFieldId"
        defaultValue={filters.subFieldId ?? ""}
        placeholder="Subfield ID"
        className="rounded-lg border border-slate-200 px-3 py-2 text-sm font-medium"
      />
      <select
        name="status"
        defaultValue={filters.status ?? ""}
        className="rounded-lg border border-slate-200 px-3 py-2 text-sm font-medium"
      >
        <option value="">All statuses</option>
        {(["PENDING", "CONFIRMED", "COMPLETED", "CANCELLED", "EXPIRED"] satisfies BookingStatus[]).map((status) => (
          <option key={status} value={status}>
            {status}
          </option>
        ))}
      </select>
      <button
        disabled={pending}
        className="rounded-lg bg-slate-950 px-4 py-2 text-sm font-bold text-white disabled:opacity-60"
      >
        Filter
      </button>
      <Link
        href="/owner/bookings"
        className="rounded-lg border border-slate-200 px-4 py-2 text-center text-sm font-bold text-slate-700"
      >
        Clear
      </Link>
    </form>
  );
}

function pageLink(page: number, filters: { bookingDate?: string; subFieldId?: string; status?: string }) {
  const params = new URLSearchParams();
  Object.entries(filters).forEach(([key, value]) => {
    if (value) params.set(key, value);
  });
  params.set("page", String(page));
  return `?${params}`;
}
