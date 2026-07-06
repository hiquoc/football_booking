import type { Metadata } from "next";
import Link from "next/link";
import { ArrowRight, CalendarRange, MapPinned, Plus } from "lucide-react";
import { BookingListContent } from "@/components/bookings/booking-list-content";
import { BackLink } from "@/components/ui/back-link";
import { PageHeading } from "@/components/ui/page-heading";

export const metadata: Metadata = { title: "Tổng quan chủ sân" };

export default function OwnerPage() {
  return (
    <>
      <BackLink href="/" className="mb-5">
        Trang chủ
      </BackLink>
      <PageHeading
        eyebrow="Khu vực chủ sân"
        title="Tổng quan hoạt động"
        description="Theo dõi lịch đặt mới và quản lý hệ thống sân của bạn."
        action={
          <Link
            href="/owner/fields/new"
            className="inline-flex items-center gap-2 rounded-full bg-sky-500 px-5 py-3 text-sm font-black text-white"
          >
            <Plus className="size-4" /> Thêm sân mới
          </Link>
        }
      />
      <div className="mt-8 grid gap-4 sm:grid-cols-2">
        <Link
          href="/owner/bookings"
          className="rounded-[1.5rem] border border-sky-100 bg-white p-6 text-slate-900"
        >
          <CalendarRange className="size-6 text-sky-400" />
          <h2 className="mt-5 text-xl font-black">Quản lý lịch đặt</h2>
          <p className="mt-2 text-sm text-slate-500">
            Xem booking mới nhất từ khách hàng.
          </p>
          <span className="mt-5 inline-flex items-center gap-2 text-sm font-black text-sky-400">
            Mở danh sách <ArrowRight className="size-4" />
          </span>
        </Link>
        <Link
          href="/owner/fields"
          className="rounded-[1.5rem] border border-slate-200 bg-white p-6"
        >
          <MapPinned className="size-6 text-sky-400" />
          <h2 className="mt-5 text-xl font-black">Hệ thống sân</h2>
          <p className="mt-2 text-sm text-slate-500">
            Quản lý thông tin, sân con và hình ảnh.
          </p>
          <span className="mt-5 inline-flex items-center gap-2 text-sm font-black text-sky-400">
            Xem sân <ArrowRight className="size-4" />
          </span>
        </Link>
      </div>
      <section className="mt-10">
        <h2 className="mb-5 text-2xl font-black text-slate-950">
          Được đặt gần đây
        </h2>
        <BookingListContent page={0} owner />
      </section>
    </>
  );
}
