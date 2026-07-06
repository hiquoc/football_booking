import { FieldManagementNav } from "@/components/owner/field-management-nav";
import { SubFieldManager } from "@/components/owner/sub-field-manager";
import { BackLink } from "@/components/ui/back-link";
import { PageHeading } from "@/components/ui/page-heading";

export default async function SubFieldsPage({
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
      <PageHeading eyebrow="Cấu hình sân" title="Sân con và bảng giá" />
      <div className="mt-8">
        <SubFieldManager fieldId={id} />
      </div>
    </div>
  );
}
