"use client";

import Link from "next/link";
import { useBookingList } from "@/lib/hooks/use-bookings";
import { useCancelBooking } from "@/lib/hooks/use-bookings";
import { BookingCard } from "./booking-card";
import { DataEmpty, DataError, ListSkeleton } from "@/components/ui/data-state";

export function BookingListContent({
  page,
  owner = false,
}: {
  page: number;
  owner?: boolean;
}) {
  const query = useBookingList(page, owner);
  const cancelMutation = useCancelBooking(true);

  if (query.isPending) return <ListSkeleton />;
  if (query.isError) return <DataError title="Không thể tải lịch đặt sân" />;
  if (!query.data.content.length)
    return (
      <DataEmpty
        title="Chưa có lịch đặt sân"
        description={
          owner
            ? "Lịch đặt của khách hàng sẽ xuất hiện tại đây."
            : "Hãy chọn một sân phù hợp và bắt đầu trận đấu đầu tiên."
        }
      />
    );

  return (
    <div className="space-y-4">
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
                  if (window.confirm("Xác nhận hủy lịch đặt này?"))
                    cancelMutation.mutate({
                      id: booking.id,
                      reason: "Chủ sân hủy lịch đặt",
                    });
                }}
                className="action-button min-h-0 rounded-lg bg-rose-500 px-3 py-2 text-xs text-white hover:bg-rose-600"
              >
                Hủy đặt
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
              href={`?page=${page}`}
            >
              Trang trước
            </Link>
          ) : null}
          {page + 1 < query.data.totalPages ? (
            <Link
              className="rounded-full bg-slate-950 px-4 py-2 text-sm font-bold text-white"
              href={`?page=${page + 2}`}
            >
              Trang sau
            </Link>
          ) : null}
        </div>
      ) : null}
    </div>
  );
}
