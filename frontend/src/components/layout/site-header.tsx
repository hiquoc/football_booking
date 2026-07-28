import { MapPinned, CalendarRange, Building2, ShieldCheck, Mail, UsersRound, Bookmark } from "lucide-react";
import Link from "next/link";
import { BrandMark } from "@/components/ui/brand-mark";
import { getCurrentUser } from "@/lib/server/session";
import { LogoutButton } from "./logout-button";
import { NotificationBell } from "@/components/notifications/notification-bell";
import { MobileMenu } from "./mobile-menu";
import { WalletBalance } from "./wallet-balance";

export async function SiteHeader() {
  const user = await getCurrentUser();
  const banTooltip = bookingBanTooltip(user);

  return (
    <header className="sticky top-0 z-50 border-b border-slate-200/70 bg-white/85 backdrop-blur-xl">
      <div className="mx-auto flex h-18 w-full max-w-[90rem] items-center justify-between px-5 sm:px-8">
        <Link href="/" aria-label="Trang chủ PitchUp">
          <BrandMark />
        </Link>
        <nav
          className="hidden items-center gap-6 text-sm font-bold text-slate-600 md:flex"
          aria-label="Điều hướng chính"
        >
          <Link className="inline-flex items-center gap-1.5 transition hover:text-sky-600" href="/fields">
            <MapPinned className="size-4" /> Tìm sân
          </Link>
          <Link className="inline-flex items-center gap-1.5 transition hover:text-sky-600" href="/community">
            <UsersRound className="size-4" /> Cộng đồng
          </Link>
          <Link className="inline-flex items-center gap-1.5 transition hover:text-sky-600" href="/contact">
            <Mail className="size-4" /> Liên hệ
          </Link>
          {user?.userType === "CLIENT" || user?.userType === "EMPLOYEE" ? (
          <Link
            className="inline-flex items-center gap-1.5 transition hover:text-sky-600"
            href={user ? "/bookings" : "/auth/login"}
          >
            <CalendarRange className="size-4" /> Lịch đặt của tôi
          </Link>
          ) : null}
          {user?.userType === "OWNER" || user?.userType === "EMPLOYEE" ? (
            <Link className="inline-flex items-center gap-1.5 transition hover:text-sky-600" href="/owner">
              <Building2 className="size-4" /> Quản lý sân
            </Link>
          ) : null}
          {user?.userType === "ADMIN" ? (
            <Link className="inline-flex items-center gap-1.5 transition hover:text-sky-600" href="/admin">
              <ShieldCheck className="size-4" /> Quản trị
            </Link>
          ) : null}
        </nav>
        {user ? (
          <div className="flex items-center gap-3">
            <NotificationBell />
            {user.userType === "CLIENT" || user.userType === "EMPLOYEE" ? (
              <Link
                href="/profile#favorite-fields"
                aria-label="Saved fields"
                title="Saved fields"
                className="grid size-10 place-items-center rounded-full border border-gray-200 bg-white !shadow-none transition hover:bg-sky-50"
              >
                <Bookmark className="size-5 text-sky-600" />
              </Link>
            ) : null}
            {user.userType === "CLIENT" || user.userType === "EMPLOYEE" ? <WalletBalance /> : null}
            <Link
              href="/profile"
              className="hidden items-center gap-2.5 rounded-full py-1 pl-1 pr-2 text-sm font-bold text-slate-700 hover:bg-sky-50 hover:text-sky-700 sm:inline-flex"
            >
              <span
                className="grid size-9 shrink-0 place-items-center rounded-full bg-sky-100 bg-cover bg-center text-xs font-black text-sky-700 ring-1 ring-sky-200"
                style={user.avatarUrl ? { backgroundImage: `url(${user.avatarUrl})` } : undefined}
                role={user.avatarUrl ? "img" : undefined}
                aria-label={user.avatarUrl ? "Ảnh đại diện" : undefined}
              >
                {user.avatarUrl
                  ? null
                  : (user.fullName || user.phoneNumber).slice(0, 1).toUpperCase()}
              </span>
              <span className="max-w-40 truncate">
                {user.fullName ?? user.phoneNumber}
              </span>
              {banTooltip ? (
                <span title={banTooltip} aria-label={banTooltip} className="size-2.5 rounded-full bg-rose-600" />
              ) : null}
            </Link>
            <div className="hidden md:block">
              <LogoutButton />
            </div>
            <MobileMenu user={user} />
          </div>
        ) : (
          <div className="flex items-center gap-3">
            <Link
              href="/auth/login"
              className="hidden rounded-full bg-sky-500 px-5 py-2.5 text-sm font-bold text-white hover:bg-sky-600 shadow-none sm:inline-flex"
            >
              Đăng nhập
            </Link>
            <MobileMenu user={null} />
          </div>
        )}
      </div>
    </header>
  );
}

function bookingBanTooltip(user: Awaited<ReturnType<typeof getCurrentUser>>) {
  if (!user?.isBookingBanned) return null;
  if (user.isPermanentBan) return "You are permanently banned from booking.";
  if (user.banExpiresAt) {
    return `You are banned from booking until ${new Intl.DateTimeFormat("en-GB", {
      day: "2-digit",
      month: "short",
      year: "numeric",
    }).format(new Date(user.banExpiresAt))}.`;
  }
  return "You are banned from booking.";
}
