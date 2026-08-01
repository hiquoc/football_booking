import { BackLink } from "@/components/ui/back-link";
import { PageHeading } from "@/components/ui/page-heading";
import { ListSkeleton } from "@/components/ui/data-state";

export default function OwnerClientViolationsLoading() {
  return (
    <>
      <BackLink href="/owner" className="mb-5">
        Quản lý sân
      </BackLink>
      <PageHeading
        eyebrow="Vi phạm"
        title="Lượt vắng mặt của khách"
        description="Theo dõi các lượt vắng mặt và cấm khách đặt sân khi cần thiết."
      />
      <div className="mt-6 grid gap-4 rounded-2xl border border-slate-200 bg-white p-5 shadow-sm sm:grid-cols-[1fr_auto]">
        <div className="space-y-2">
          <div className="h-4 w-16 rounded bg-slate-100" />
          <div className="h-11 rounded-xl bg-slate-100" />
        </div>
        <div className="h-11 w-24 rounded-xl bg-slate-100 sm:self-end" />
      </div>
      <div className="mt-6">
        <ListSkeleton />
      </div>
    </>
  );
}
