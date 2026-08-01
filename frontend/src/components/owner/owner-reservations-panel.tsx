"use client";

import Link from "next/link";
import { useSearchParams } from "next/navigation";
import { BookingCard } from "@/components/bookings/booking-card";
import { DataEmpty, DataError, ListSkeleton } from "@/components/ui/data-state";
import { useCancelReservation, useOwnerReservations } from "@/lib/hooks/use-bookings";

export function OwnerReservationsPanel({ fieldId }: { fieldId?: string }) {
  const searchParams = useSearchParams();
  const page = Math.max(0, (Number(searchParams.get("page")) || 1) - 1);
  const filters = {
    bookingDate: searchParams.get("bookingDate")?.trim() || undefined,
    subFieldId: searchParams.get("subFieldId")?.trim() || undefined,
    status: searchParams.get("status")?.trim() || undefined,
  };
  const query = useOwnerReservations(page, 10, filters);
  const cancelMutation = useCancelReservation();

  if (query.isPending) return <ListSkeleton />;
  if (query.isError) return <DataError title="Không thể tải lịch giữ sân" />;
  if (!query.data.content.length) {
    return (
      <DataEmpty
        title="Chưa có lịch giữ sân"
        description="Các khung giờ chủ sân tự giữ sẽ hiển thị tại đây."
      />
    );
  }

  return (
    <div className="space-y-5">
      {query.data.content.map((reservation) => (
        <BookingCard
          key={reservation.id}
          booking={reservation}
          owner
          action={
            reservation.status === "PENDING" || reservation.status === "CONFIRMED" ? (
              <button
                disabled={cancelMutation.isPending}
                onClick={() => {
                  if (window.confirm("Xác nhận hủy lịch giữ sân này?")) {
                    cancelMutation.mutate({
                      id: reservation.id,
                      reason: "Chủ sân đã hủy lịch giữ sân",
                    });
                  }
                }}
                className="action-button min-h-0 rounded-lg bg-rose-500 px-3 py-2 text-xs text-white"
              >
                Hủy lịch
              </button>
            ) : undefined
          }
        />
      ))}
      {fieldId ? (
        <Link
          href={`/fields/${fieldId}/book?mode=reservation`}
          className="action-button w-fit bg-green-600 px-5 text-white hover:bg-green-700"
        >
          Tạo lịch giữ sân
        </Link>
      ) : null}
    </div>
  );
}
