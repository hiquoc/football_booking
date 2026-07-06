import { AdminFieldsPanel } from "@/components/admin/admin-fields-panel";
import { BackLink } from "@/components/ui/back-link";
import { PageHeading } from "@/components/ui/page-heading";

export default function AdminFieldsPage() {
  return (
    <>
      <BackLink href="/admin" className="mb-5">
        Bảng điều khiển
      </BackLink>
      <PageHeading
        eyebrow="Kiểm duyệt"
        title="Danh sách sân"
        description="Theo dõi sân đang chờ xác nhận, đã xác nhận hoặc đã từ chối."
      />
      <AdminFieldsPanel />
    </>
  );
}
