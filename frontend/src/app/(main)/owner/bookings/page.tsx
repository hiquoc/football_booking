import type { Metadata } from "next";
import { OwnerBookingsPanel } from "@/components/owner/owner-bookings-panel";
import { PageHeading } from "@/components/ui/page-heading";
import { BackLink } from "@/components/ui/back-link";

export const metadata: Metadata = { title: "Lịch đặt của sân" };

export default function OwnerBookingsPage() {
  return (
    <>
      <BackLink href="/owner" className="mb-5">
        Quản lý sân
      </BackLink>
      <PageHeading
        eyebrow="Vận hành"
        title="Lịch đặt của khách"
        description="Theo dõi các lịch đặt thuộc hệ thống sân của bạn."
      />
      <div className="mt-8">
        <OwnerBookingsPanel />
      </div>
    </>
  );
}
