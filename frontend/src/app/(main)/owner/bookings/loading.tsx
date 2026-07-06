import { BackLink } from "@/components/ui/back-link";
import { ListSkeleton } from "@/components/ui/data-state";
import { PageHeading } from "@/components/ui/page-heading";

export default function OwnerBookingsLoading() {
  return (
    <>
      <BackLink href="/owner" className="mb-5">
        Quản lý sân
      </BackLink>
      <PageHeading
        eyebrow="Vận hành"
        title="Lịch đặt của khách"
        description="Theo dõi các booking thuộc hệ thống sân của bạn."
      />
      <div className="mt-8">
        <ListSkeleton />
      </div>
    </>
  );
}
