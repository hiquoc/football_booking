import type { Metadata } from "next";
import { FieldCreateForm } from "@/components/owner/field-create-form";
import { BackLink } from "@/components/ui/back-link";
import { PageHeading } from "@/components/ui/page-heading";

export const metadata: Metadata = { title: "Thêm sân mới" };

export default function NewFieldPage() {
  return (
    <>
      <BackLink href="/owner/fields" className="mb-5">
        Sân của tôi
      </BackLink>
      <PageHeading
        eyebrow="Thiết lập địa điểm"
        title="Thêm sân mới"
        description="Sân mới sẽ ở trạng thái chờ quản trị viên phê duyệt trước khi hiển thị công khai."
      />
      <div className="mt-8">
        <FieldCreateForm />
      </div>
    </>
  );
}
