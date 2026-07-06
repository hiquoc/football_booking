import { FieldEditor } from "@/components/owner/field-editor";
import { FieldManagementNav } from "@/components/owner/field-management-nav";
import { BackLink } from "@/components/ui/back-link";
import { PageHeading } from "@/components/ui/page-heading";

export default async function EditFieldPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = await params;
  return (
    <div className="w-full py-2">
      <BackLink href="/owner/fields" className="mb-5">
        Sân của tôi
      </BackLink>
      <FieldManagementNav fieldId={id} />
      <PageHeading eyebrow="Quản lý sân" title="Chỉnh sửa thông tin" />
      <div className="mt-8">
        <FieldEditor fieldId={id} />
      </div>
    </div>
  );
}
