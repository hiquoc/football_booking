import Link from "next/link";
import { BrandMark } from "@/components/ui/brand-mark";

export function SiteFooter() {
  return (
    <footer className="border-t border-slate-200 bg-white">
      <div className="mx-auto flex w-full max-w-[90rem] flex-col gap-4 px-5 py-7 sm:px-8 md:h-20 md:flex-row md:items-center md:justify-between md:py-0">
        <Link href="/" aria-label="Trang chủ PitchUp">
          <BrandMark />
        </Link>
        <nav className="flex flex-wrap gap-4 text-sm font-bold text-slate-500">
          <Link className="hover:text-green-700" href="/fields">Tìm sân</Link>
          <Link className="hover:text-green-700" href="/contact">Liên hệ</Link>
          <Link className="hover:text-green-700" href="/bookings">Lịch đặt</Link>
          <Link className="hover:text-green-700" href="/profile">Hồ sơ</Link>
        </nav>
        <p className="text-xs font-semibold uppercase text-slate-400">
          TP. Hồ Chí Minh · Việt Nam
        </p>
      </div>
    </footer>
  );
}
