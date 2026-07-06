import { BackLink } from "@/components/ui/back-link";
import { ListSkeleton } from "@/components/ui/data-state";
import { PageHeading } from "@/components/ui/page-heading";

export default function OwnerFieldsLoading() {
  return (
    <>
      <BackLink href="/owner" className="mb-5">
        Quản lý sân
      </BackLink>
      <PageHeading eyebrow="Địa điểm" title="Sân của tôi" />
      <div className="mt-8">
        <div className="mb-4 h-5 w-32 animate-pulse rounded bg-slate-200" />
        <ListSkeleton />
      </div>
    </>
  );
}
