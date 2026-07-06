import { ListSkeleton } from "@/components/ui/data-state";
import { PageHeading } from "@/components/ui/page-heading";

export default function AdminFieldsLoading() {
  return (
    <>
      <PageHeading
        eyebrow="Kiểm duyệt"
        title="Danh sách sân"
        description="Theo dõi sân đang chờ xác nhận, đã xác nhận hoặc đã từ chối."
      />
      <div className="mt-8">
        <div className="mb-6 flex gap-2">
          {[0, 1, 2].map((item) => (
            <div
              key={item}
              className="h-10 w-32 animate-pulse rounded-full bg-slate-200"
            />
          ))}
        </div>
        <ListSkeleton />
      </div>
    </>
  );
}
