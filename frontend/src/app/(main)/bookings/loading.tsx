import { ListSkeleton } from "@/components/ui/data-state";
import { PageHeading } from "@/components/ui/page-heading";

export default function BookingsLoading() {
  return (
    <div className="mx-auto min-h-[70vh] w-full max-w-[90rem] px-5 py-12 sm:px-8">
      <PageHeading eyebrow="Quản lý trận đấu" title="Lịch đặt của tôi" />
      <div className="mt-8"><ListSkeleton /></div>
    </div>
  );
}
