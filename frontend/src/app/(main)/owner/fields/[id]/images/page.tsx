import { FieldManagementNav } from "@/components/owner/field-management-nav";
import { ImageManager } from "@/components/owner/image-manager";
import { BackLink } from "@/components/ui/back-link";
import { PageHeading } from "@/components/ui/page-heading";

export default async function ImagesPage({
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
      <PageHeading eyebrow="Thư viện" title="Quản lý hình ảnh" />
      <div className="mt-8">
        <ImageManager fieldId={id} />
      </div>
    </div>
  );
}
