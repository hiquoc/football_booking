import { ClientViolationsPanel } from "@/components/owner/client-violations-panel";
import { BackLink } from "@/components/ui/back-link";
import { PageHeading } from "@/components/ui/page-heading";
import { getAssignedFields, getOwnerFields } from "@/lib/server/fields";
import { requireUser } from "@/lib/server/guards";

const getManagedFieldsForViolationPage = (role: string) =>
  role === "EMPLOYEE" ? getAssignedFields(0, 100) : getOwnerFields(0, 100);

export default async function OwnerClientViolationsPage({
  searchParams,
}: {
  searchParams: Promise<{ fieldId?: string; page?: string }>;
}) {
  const user = await requireUser();
  const query = await searchParams;
  const fields = await getManagedFieldsForViolationPage(user.userType);
  const allowedFields = fields.content;
  const requestedFieldId = query.fieldId;
  const selectedFieldId = allowedFields.some((field) => field.id === requestedFieldId)
    ? requestedFieldId
    : allowedFields[0]?.id;
  const requestedDenied = Boolean(requestedFieldId && requestedFieldId !== selectedFieldId);
  const page = Math.max(0, Number(query.page) || 0);

  return (
    <>
      <BackLink href="/owner" className="mb-5">
        Quản lý sân
      </BackLink>
      <PageHeading
        eyebrow="Vi phạm"
        title="Lượt vắng mặt của khách"
        description="Theo dõi các lượt vắng mặt và cấm khách đặt sân khi cần thiết."
      />
      <ClientViolationsPanel
        key={`${selectedFieldId ?? ""}-${page}`}
        fields={allowedFields}
        selectedFieldId={selectedFieldId ?? ""}
        page={page}
        requestedDenied={requestedDenied}
      />
    </>
  );
}
