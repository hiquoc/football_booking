"use client";

import Link from "next/link";
import { MapPinned, CalendarRange, UserRound, Building2, ShieldCheck, Menu, X, Mail, UsersRound } from "lucide-react";
import { useState } from "react";
import type { User } from "@/lib/api/types";

export function MobileMenu({ user }: { user: User | null }) {
  const [open, setOpen] = useState(false);
  const links = [
    { href: "/fields", label: "Tìm sân", icon: MapPinned, show: true },
    { href: "/community", label: "Cộng đồng", icon: UsersRound, show: true },
    { href: "/contact", label: "Liên hệ hợp tác", icon: Mail, show: true },
    { href: user ? "/bookings" : "/auth/login", label: "Lịch đặt của tôi", icon: CalendarRange, show: true },
    { href: "/profile", label: "Hồ sơ cá nhân", icon: UserRound, show: Boolean(user) },
    { href: "/owner", label: "Quản lý sân", icon: Building2, show: user?.userType === "OWNER" || user?.userType === "EMPLOYEE" },
    {
      href: "/admin",
      label: "Quản trị hệ thống",
      icon: ShieldCheck,
      show: user?.userType === "ADMIN",
    },
  ];
  return (
    <div className="md:hidden">
      <button
        onClick={() => setOpen((value) => !value)}
        aria-label="Mở menu"
        className="grid size-10 place-items-center rounded-full border border-slate-200 text-slate-700 hover:bg-green-50 hover:text-green-700"
      >
        {open ? <X className="size-5" /> : <Menu className="size-5" />}
      </button>
      {open ? (
        <div className="absolute left-0 right-0 top-full border-b border-slate-200 bg-white p-5 shadow-xl">
          <nav className="mx-auto flex max-w-7xl flex-col gap-1">
              {links
                .filter((item) => item.show)
                .map(({ href, label, icon: Icon }) => (
                  <Link
                    key={href}
                    href={href}
                    onClick={() => setOpen(false)}
                    className="inline-flex items-center gap-3 rounded-xl px-4 py-3 text-sm font-bold text-slate-700 hover:bg-green-50 hover:text-green-700"
                  >
                    <Icon className="size-4 text-green-600" /> {label}
                  </Link>
                ))}
            {!user ? (
              <Link
                href="/auth/login"
                className="mt-2 rounded-xl bg-green-600 px-4 py-3 text-center text-sm font-black text-white hover:bg-green-700"
              >
                Đăng nhập
              </Link>
            ) : null}
          </nav>
        </div>
      ) : null}
    </div>
  );
}
