import { ClosureManager } from "@/components/owner/closure-manager";
import { FieldManagementNav } from "@/components/owner/field-management-nav";
import { BackLink } from "@/components/ui/back-link";
import { PageHeading } from "@/components/ui/page-heading";

export default async function ClosuresPage({
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
      <PageHeading eyebrow="Vận hành" title="Lịch đóng sân" />
      <div className="mt-8">
        <ClosureManager fieldId={id} />
      </div>
    </div>
  );
}
