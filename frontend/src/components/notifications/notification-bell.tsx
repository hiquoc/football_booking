"use client";

import { useEffect, useRef, useState } from "react";
import { Bell, X } from "lucide-react";
import { NotificationList } from "@/components/notifications/notification-list";
import { useNotificationSocket } from "@/lib/hooks/use-notification-socket";
import { useUnreadNotificationCount } from "@/lib/hooks/use-notifications";

export function NotificationBell() {
  useNotificationSocket();
  const count = useUnreadNotificationCount();
  const value = count.data?.count ?? 0;
  const [open, setOpen] = useState(false);
  const containerRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!open) return;

    function closeOnOutsideClick(event: PointerEvent) {
      if (!containerRef.current?.contains(event.target as Node)) setOpen(false);
    }

    function closeOnEscape(event: KeyboardEvent) {
      if (event.key === "Escape") setOpen(false);
    }

    document.addEventListener("pointerdown", closeOnOutsideClick);
    document.addEventListener("keydown", closeOnEscape);
    return () => {
      document.removeEventListener("pointerdown", closeOnOutsideClick);
      document.removeEventListener("keydown", closeOnEscape);
    };
  }, [open]);

  return (
    <div ref={containerRef} className="relative">
      <button
        type="button"
        aria-label={`${value} thông báo chưa đọc`}
        aria-expanded={open}
        aria-controls="notification-panel"
        onClick={() => setOpen((current) => !current)}
        className="relative grid size-10 place-items-center rounded-full border border-slate-200 text-slate-600 transition hover:border-sky-300 hover:text-sky-600"
      >
        <Bell className="size-4" />
        {value > 0 ? (
          <span className="absolute -right-1 -top-1 grid min-w-5 place-items-center rounded-full bg-rose-500 px-1 text-[10px] font-black leading-5 text-white">
            {value > 99 ? "99+" : value}
          </span>
        ) : null}
      </button>

      {open ? (
        <section
          id="notification-panel"
          aria-label="Thông báo"
          className="fixed inset-x-3 top-20 z-[70] max-h-[calc(100vh-6rem)] overflow-hidden rounded-3xl border border-slate-200 bg-white p-4 shadow-2xl shadow-slate-900/15 sm:absolute sm:inset-x-auto sm:right-0 sm:top-12 sm:w-[30rem]"
        >
          <div className="mb-3 flex items-center justify-between">
            <div>
              <h2 className="font-display text-lg font-black text-slate-950">Thông báo</h2>
              <p className="text-xs text-slate-500">{value} thông báo chưa đọc</p>
            </div>
            <button
              type="button"
              aria-label="Đóng bảng thông báo"
              onClick={() => setOpen(false)}
              className="grid size-9 place-items-center rounded-full text-slate-500 transition hover:bg-slate-100 hover:text-slate-900"
            >
              <X className="size-4" />
            </button>
          </div>
          <div className="max-h-[calc(100vh-11rem)] overflow-y-auto overscroll-contain pr-1 sm:max-h-[32rem]">
            <NotificationList compact onNavigate={() => setOpen(false)} />
          </div>
        </section>
      ) : null}
    </div>
  );
}
