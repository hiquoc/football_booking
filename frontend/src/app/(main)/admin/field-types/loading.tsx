import { ListSkeleton } from "@/components/ui/data-state";
import { PageHeading } from "@/components/ui/page-heading";

export default function AdminFieldTypesLoading() {
  return (
    <>
      <PageHeading
        eyebrow="Danh mục"
        title="Quản lý loại sân"
        description="Thiết lập môn thể thao và thời lượng đặt mặc định."
      />
      <div className="mt-8">
        <ListSkeleton />
      </div>
    </>
  );
}
