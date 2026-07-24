"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useTransition, type FormEvent } from "react";
import type { BookingStatus } from "@/lib/api/types";
import { getBookingStatus } from "@/lib/booking-format";
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
  if (query.isError) return <DataError title="Không thể tải danh sách lịch đặt" />;
  if (!query.data.content.length) {
    return (
      <>
        {owner ? <OwnerFilters filters={filters} /> : null}
        <DataEmpty
          title="Chưa có lịch đặt"
          description={
            owner
              ? "Lịch đặt của khách hàng sẽ hiển thị tại đây."
              : "Chọn sân phù hợp để bắt đầu trận đấu đầu tiên của bạn."
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
                  if (window.confirm("Xác nhận hủy lịch đặt này?")) {
                    cancelMutation.mutate({
                      id: booking.id,
                      reason: "Chủ sân đã hủy lịch đặt",
                    });
                  }
                }}
                className="action-button min-h-0 rounded-lg bg-rose-500 px-3 py-2 text-xs text-white hover:bg-rose-600"
              >
                Hủy lịch
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
              Trước
            </Link>
          ) : null}
          {page + 1 < query.data.totalPages ? (
            <Link
              className="rounded-full bg-slate-950 px-4 py-2 text-sm font-bold text-white"
              href={pageLink(page + 2, filters)}
            >
              Sau
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
        placeholder="Mã sân con"
        className="rounded-lg border border-slate-200 px-3 py-2 text-sm font-medium"
      />
      <select
        name="status"
        defaultValue={filters.status ?? ""}
        className="rounded-lg border border-slate-200 px-3 py-2 text-sm font-medium"
      >
        <option value="">Tất cả trạng thái</option>
        {(["PENDING", "CONFIRMED", "COMPLETED", "CANCELLED", "EXPIRED"] satisfies BookingStatus[]).map((status) => (
          <option key={status} value={status}>
            {getBookingStatus(status).label}
          </option>
        ))}
      </select>
      <button
        disabled={pending}
        className="rounded-lg bg-slate-950 px-4 py-2 text-sm font-bold text-white disabled:opacity-60"
      >
        Lọc
      </button>
      <Link
        href="/owner/bookings"
        className="rounded-lg border border-slate-200 px-4 py-2 text-center text-sm font-bold text-slate-700"
      >
        Xóa lọc
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
