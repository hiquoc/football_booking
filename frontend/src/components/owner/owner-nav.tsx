import { CalendarRange, ClipboardList, Gavel, LayoutDashboard, MapPinned, ShieldAlert } from "lucide-react";
import Link from "next/link";
import { getCurrentUser } from "@/lib/server/session";

export async function OwnerNav() {
  const user = await getCurrentUser();
  const showOwnerOnly = user?.userType === "OWNER";
  const links = [
    { href: "/owner", label: "Tổng quan", icon: LayoutDashboard, show: true },
    { href: "/owner/fields", label: "Sân quản lý", icon: MapPinned, show: true },
    { href: "/owner/bookings", label: "Lịch đặt", icon: CalendarRange, show: true },
    { href: "/owner/recurring-bookings", label: "Đặt định kỳ", icon: ClipboardList, show: true },
    { href: "/owner/client-violations", label: "Vi phạm", icon: ShieldAlert, show: true },
    { href: "/owner/payment-disputes", label: "Tranh chấp", icon: Gavel, show: showOwnerOnly },
  ];

  return (
    <nav className="border-b border-slate-200 bg-white" aria-label="Quản lý sân">
      <div className="mx-auto flex w-full max-w-[90rem] gap-2 overflow-x-auto px-5 py-3 sm:px-8">
        {links.filter((link) => link.show).map((link) => {
          const Icon = link.icon;
          return (
            <Link
              key={link.href}
              href={link.href}
              className="inline-flex shrink-0 items-center gap-2 rounded-lg px-3 py-2 text-sm font-bold text-slate-600 transition hover:bg-sky-50 hover:text-sky-700"
            >
              <Icon className="size-4" />
              {link.label}
            </Link>
          );
        })}
      </div>
    </nav>
  );
}
