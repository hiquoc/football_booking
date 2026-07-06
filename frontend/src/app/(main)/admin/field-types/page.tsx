import { FieldTypeManager } from "@/components/admin/field-type-manager";
import { BackLink } from "@/components/ui/back-link";
import { PageHeading } from "@/components/ui/page-heading";

export default function FieldTypesPage() {
  return (
    <>
      <BackLink href="/admin" className="mb-5">
        Bảng điều khiển
      </BackLink>
      <PageHeading
        eyebrow="Danh mục"
        title="Quản lý loại sân"
        description="Thiết lập môn thể thao và thời lượng đặt mặc định."
      />
      <div className="mt-8">
        <FieldTypeManager />
      </div>
    </>
  );
}
