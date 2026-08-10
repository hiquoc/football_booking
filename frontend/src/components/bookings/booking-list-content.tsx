"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useTransition } from "react";
import type { BookingStatus } from "@/lib/api/types";
import { getBookingStatus } from "@/lib/booking-format";
import { formatFieldType } from "@/lib/field-format";
import { useBookingList, useCancelBooking } from "@/lib/hooks/use-bookings";
import { useSubFieldTypes } from "@/lib/hooks/use-field-types";
import { useCurrentManagedFields } from "@/lib/hooks/use-owner-fields";
import { DataEmpty, DataError, ListSkeleton } from "@/components/ui/data-state";
import { BookingCard } from "./booking-card";

type BookingFiltersValue = {
  bookingDate?: string;
  fieldId?: string;
  fieldType?: string;
  subFieldId?: string;
  subFieldType?: string;
  status?: string;
};

export function BookingListContent({
  page,
  owner = false,
  filters = {},
  bookingDateCleared = false,
}: {
  page: number;
  owner?: boolean;
  filters?: BookingFiltersValue;
  bookingDateCleared?: boolean;
}) {
  const query = useBookingList(page, owner, 10, filters);
  const cancelMutation = useCancelBooking(true);

  if (query.isPending) {
    return (
      <>
        <BookingFilters owner={owner} filters={filters} />
        <ListSkeleton />
      </>
    );
  }

  if (query.isError) {
    return <DataError title="Không thể tải danh sách lịch đặt" />;
  }

  if (!query.data.content.length) {
    return (
      <>
        <BookingFilters owner={owner} filters={filters} />
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
    <div className="space-y-5">
      <BookingFilters owner={owner} filters={filters} />
      {query.data.content.map((booking) => (
        <BookingCard
          key={booking.id}
          booking={booking}
          owner={owner}
          action={
            owner ? (
              <>
                {booking.status === "PENDING" || booking.status === "CONFIRMED" ? (
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
                    className="action-button min-h-0 rounded-lg bg-rose-500 px-3 py-2 text-xs text-white"
                  >
                    Hủy lịch
                  </button>
                ) : null}
              </>
            ) : undefined
          }
        />
      ))}
      {query.data.totalPages > 1 ? (
        <div className="flex justify-center gap-3 pt-5">
          {page > 0 ? (
            <Link
              className="rounded-xl border border-slate-200 bg-white px-4 py-2 text-sm font-bold text-slate-700 hover:border-green-300 hover:bg-green-50 hover:text-green-700"
              href={pageLink(page, filters, bookingDateCleared)}
            >
              Trước
            </Link>
          ) : null}
          {page + 1 < query.data.totalPages ? (
            <Link
              className="rounded-xl bg-green-600 px-4 py-2 text-sm font-bold text-white"
              href={pageLink(page + 2, filters, bookingDateCleared)}
            >
              Sau
            </Link>
          ) : null}
        </div>
      ) : null}
    </div>
  );
}

function BookingFilters({
  owner,
  filters,
}: {
  owner: boolean;
  filters: BookingFiltersValue;
}) {
  const router = useRouter();
  const [, startTransition] = useTransition();
  const fields = owner? useCurrentManagedFields(0, 100) : undefined;
  const subFieldTypes = useSubFieldTypes();
  const fieldTypeOptions =owner ? Array.from(
    new Set(
      fields?.data?.content.flatMap((field) =>
        field.fieldTypes.map((type) => type.name),
      ) ?? [],
    ),
  ) : [];

  function applyFilters(formElement: HTMLFormElement) {
    const form = new FormData(formElement);
    const params = new URLSearchParams();
    ["bookingDate", "fieldId", "fieldType", "subFieldType", "status"].forEach((key) => {
      const value = String(form.get(key) ?? "").trim();
      if (value) params.set(key, value);
    });
    if (owner && !String(form.get("bookingDate") ?? "").trim()) {
      params.set("bookingDateCleared", "1");
    }
    startTransition(() =>
      router.push(`${owner ? "/owner/bookings" : "/bookings"}${params.size ? `?${params}` : ""}`),
    );
  }

  return (
    <form
      onSubmit={(event) => {
        event.preventDefault();
        applyFilters(event.currentTarget);
      }}
      onChange={(event) => applyFilters(event.currentTarget)}
      className={`grid gap-4 rounded-2xl border border-slate-200 bg-white p-5 shadow-sm ${
        owner ? "md:grid-cols-[1fr_1fr_1fr_1fr_auto]" : "md:grid-cols-[1fr_1fr_auto]"
      }`}
    >
      <input
        name="bookingDate"
        type="date"
        defaultValue={filters.bookingDate ?? ""}
        className="min-h-11 rounded-xl border border-slate-200 bg-slate-50 px-3 py-2 text-sm font-semibold text-slate-700 outline-none focus:border-green-500 focus:bg-white focus:ring-4 focus:ring-green-100"
      />
      {owner ? (
        <select
          name="fieldId"
          defaultValue={filters.fieldId ?? ""}
          className="min-h-11 rounded-xl border border-slate-200 bg-slate-50 px-3 py-2 text-sm font-semibold text-slate-700 outline-none focus:border-green-500 focus:bg-white focus:ring-4 focus:ring-green-100 disabled:text-slate-400"
          disabled={fields?.isPending || fields?.isError}
        >
          <option value="">
            {fields?.isPending
              ? "Đang tải sân..."
              : fields?.isError
                ? "Không thể tải sân"
                : "Tất cả sân"}
          </option>
          {fields?.data?.content.map((field) => (
            <option key={field.id} value={field.id}>
              {field.name}
            </option>
          ))}
        </select>
      ) : null}
      {owner ? (
        <select
          name="fieldType"
          defaultValue={filters.fieldType ?? ""}
          className="min-h-11 rounded-xl border border-slate-200 bg-slate-50 px-3 py-2 text-sm font-semibold text-slate-700 outline-none focus:border-green-500 focus:bg-white focus:ring-4 focus:ring-green-100 disabled:text-slate-400"
          disabled={fields?.isPending || fields?.isError}
        >
          <option value="">Tất cả môn</option>
          {fieldTypeOptions.map((type) => (
            <option key={type} value={type}>
              {formatFieldType(type)}
            </option>
          ))}
        </select>
      ) : null}
      {owner ? (
        <select
          name="subFieldType"
          defaultValue={filters.subFieldType ?? ""}
          className="min-h-11 rounded-xl border border-slate-200 bg-slate-50 px-3 py-2 text-sm font-semibold text-slate-700 outline-none focus:border-green-500 focus:bg-white focus:ring-4 focus:ring-green-100 disabled:text-slate-400"
          disabled={subFieldTypes?.isPending || subFieldTypes?.isError}
        >
          <option value="">Tất cả loại sân</option>
          {subFieldTypes?.data?.map((type) => (
            <option key={type} value={type}>
              {formatFieldType(type)}
            </option>
          ))}
        </select>
      ) : null}
      {owner && fields?.isSuccess && fields.data.content.length === 0 ? (
        <p className="rounded-lg border border-amber-200 bg-amber-50 px-3 py-2 text-sm font-semibold text-amber-800 md:col-span-5">
          Chưa có sân để lọc.
        </p>
      ) : null}
      <select
        name="status"
        defaultValue={filters.status ?? ""}
        className="min-h-11 rounded-xl border border-slate-200 bg-slate-50 px-3 py-2 text-sm font-semibold text-slate-700 outline-none focus:border-green-500 focus:bg-white focus:ring-4 focus:ring-green-100"
      >
        <option value={owner ? "ALL" : ""}>Tất cả trạng thái</option>
        {(["PENDING", "CONFIRMED", "COMPLETED", "REPORTED", "CANCELLED", "EXPIRED"] satisfies BookingStatus[]).map((status) => (
          <option key={status} value={status}>
            {getBookingStatus(status).label}
          </option>
        ))}
      </select>
    </form>
  );
}

function pageLink(page: number, filters: BookingFiltersValue, bookingDateCleared = false) {
  const params = new URLSearchParams();
  Object.entries(filters).forEach(([key, value]) => {
    if (value) params.set(key, value);
  });
  if (bookingDateCleared) params.set("bookingDateCleared", "1");
  params.set("page", String(page));
  return `?${params}`;
}
