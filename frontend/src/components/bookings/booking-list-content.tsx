"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useTransition } from "react";
import type { BookingStatus } from "@/lib/api/types";
import { getBookingStatus } from "@/lib/booking-format";
import { useBookingList, useCancelBooking } from "@/lib/hooks/use-bookings";
import { useSubFieldFilterOptions } from "@/lib/hooks/use-fields";
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
  const subFieldOptions = useSubFieldFilterOptions();
  const hasActiveFilters = Boolean(filters.bookingDate || filters.subFieldId || filters.status);

  function applyFilters(formElement: HTMLFormElement) {
    const form = new FormData(formElement);
    const params = new URLSearchParams();
    ["bookingDate", "subFieldId", "status"].forEach((key) => {
      const value = String(form.get(key) ?? "").trim();
      if (value) params.set(key, value);
    });
    startTransition(() => router.push(`/owner/bookings${params.size ? `?${params}` : ""}`));
  }

  return (
    <form
      onSubmit={(event) => {
        event.preventDefault();
        applyFilters(event.currentTarget);
      }}
      onChange={(event) => applyFilters(event.currentTarget)}
      className="grid gap-3 rounded-lg border border-slate-200 bg-white p-4 shadow-sm md:grid-cols-[1fr_1fr_1fr_auto]"
    >
      <input
        name="bookingDate"
        type="date"
        defaultValue={filters.bookingDate ??  new Date().toISOString().split("T")[0]}
        className="rounded-lg border border-slate-200 px-3 py-2 text-sm font-medium"
      />
      <select
        name="subFieldId"
        defaultValue={filters.subFieldId ?? ""}
        className="rounded-lg border border-slate-200 px-3 py-2 text-sm font-medium"
        disabled={subFieldOptions.isPending || subFieldOptions.isError}
      >
        <option value="">
          {subFieldOptions.isPending
            ? "Đang tải sân con..."
            : subFieldOptions.isError
              ? "Không thể tải sân con"
              : "Tất cả sân con"}
        </option>
        {subFieldOptions.data?.map((option) => (
          <option key={option.id} value={option.id}>
            {option.name}
          </option>
        ))}
      </select>
      {subFieldOptions.isSuccess && subFieldOptions.data.length === 0 ? (
        <p className="rounded-lg border border-amber-200 bg-amber-50 px-3 py-2 text-sm font-semibold text-amber-800 md:col-span-5">
          Chưa có sân con để lọc.
        </p>
      ) : null}
      <select
        name="status"
        defaultValue={filters.status ?? "COMPLETED"}
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
        className={`rounded-lg border px-4 py-2 text-sm font-bold disabled:opacity-60 ${
          hasActiveFilters
            ? "border-slate-950 bg-slate-950 text-white"
            : "border-slate-200 bg-white text-slate-700"
        }`}
      >
        Lọc
      </button>
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
