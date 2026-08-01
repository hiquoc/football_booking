"use client";

import Link from "next/link";
import { ArrowUpRight, Bell, Check, CheckCheck, LoaderCircle } from "lucide-react";
import { formatDate } from "@/lib/field-format";
import {
  useMarkAllNotificationsRead,
  useMarkNotificationRead,
  useNotifications,
} from "@/lib/hooks/use-notifications";
import { DataEmpty, DataError, ListSkeleton } from "@/components/ui/data-state";
import { formatNotification } from "@/lib/notification-format";

type NotificationListProps = {
  page?: number;
  compact?: boolean;
  onNavigate?: () => void;
};

export function NotificationList({ page = 0, compact = false, onNavigate }: NotificationListProps) {
  const notifications = useNotifications(page, compact ? 10 : 20);
  const markOne = useMarkNotificationRead();
  const markAll = useMarkAllNotificationsRead();
  if (notifications.isPending) return <ListSkeleton count={5} />;
  if (notifications.isError)
    return <DataError title="Không thể tải thông báo" />;
  if (!notifications.data.content.length)
    return (
      <DataEmpty
        title="Bạn chưa có thông báo"
        description="Các cập nhật về lịch đặt và thanh toán sẽ xuất hiện tại đây."
      />
    );
  const hasUnread = notifications.data.content.some((item) => !item.isRead);
  const mutationError = markOne.error ?? markAll.error;
  return (
    <div>
      <div className={`flex justify-end ${compact ? "mb-3" : "mb-4"}`}>
        {hasUnread ? (
          <button
            onClick={() => markAll.mutate()}
            disabled={markAll.isPending}
            className={`action-button min-h-0 bg-green-50 text-green-700 hover:bg-green-100 ${compact ? "px-3 py-2 text-xs" : "px-4 py-2.5 text-sm"}`}
          >
            {markAll.isPending ? (
              <LoaderCircle className="size-4 animate-spin" />
            ) : (
              <CheckCheck className="size-4" />
            )}{" "}
            Đánh dấu tất cả đã đọc
          </button>
        ) : null}
      </div>
      <div className={`overflow-hidden border border-slate-200 bg-white ${compact ? "rounded-2xl" : "rounded-[1.5rem]"}`}>
        {notifications.data.content.map((item) => {
          const content = formatNotification(item);
          const detailHref = notificationDetailHref(item);
          return (
            <article
              key={item.id}
              className={`flex w-full border-b border-slate-100 text-left last:border-0 ${compact ? "gap-3 p-3" : "gap-4 p-5"} ${item.isRead ? "bg-white" : "bg-green-50/60"}`}
            >
              <span
                className={`grid shrink-0 place-items-center rounded-xl ${compact ? "size-9" : "size-10"} ${item.isRead ? "bg-slate-100 text-slate-400" : "bg-green-600 text-white"}`}
              >
                <Bell className="size-4" />
              </span>
              <span className="min-w-0 flex-1">
                <strong className="block text-sm text-slate-900">
                  {content.title}
                </strong>
                {content.detail ? <span className="mt-1 block text-sm leading-5 text-slate-600">{content.detail}</span> : null}
                <span className="mt-1 block text-xs text-slate-400">
                  {formatDate(item.createdAt)}
                </span>
              </span>
              <span className="flex shrink-0 flex-col items-end gap-2">
                {!item.isRead ? (
                  <button
                    type="button"
                    onClick={() => markOne.mutate(item.id)}
                    disabled={markOne.isPending}
                    className="action-button !min-h-0 h-8 border border-green-200 bg-white px-2 py-1 text-xs text-green-700 hover:bg-green-50 !shadow-none"
                  >
                    <Check className="size-3.5" /> Đã đọc
                  </button>
                ) : (
                  <span className="action-button invisible !min-h-0 h-8 px-2 py-1 text-xs">
                    <Check className="size-3.5" /> Đã đọc
                  </span>
                )}

                {detailHref ? (
                  <Link
                    onClick={onNavigate}
                    href={detailHref}
                    className="action-button !min-h-0 h-8 bg-slate-950 p-1 text-xs text-white hover:bg-slate-800"
                  >
                    Chi tiết <ArrowUpRight className="size-3.5" />
                  </Link>
                ) : null}
              </span>
            </article>
          );
        })}
      </div>
      {mutationError ? <p role="alert" className="mt-4 rounded-xl bg-rose-50 p-3 text-sm text-rose-700">Không thể cập nhật trạng thái thông báo</p> : null}
    </div>
  );
}

function notificationDetailHref(item: { code: string; payload: Record<string, unknown> }) {
  const postId = typeof item.payload.postId === "string" ? item.payload.postId : null;
  if (item.code.startsWith("COMMUNITY_") && postId) {
    return `/community/${encodeURIComponent(postId)}`;
  }
  const bookingId = typeof item.payload.bookingId === "string" ? item.payload.bookingId : null;
  return bookingId ? `/bookings/${encodeURIComponent(bookingId)}` : null;
}
