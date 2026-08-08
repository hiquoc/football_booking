import { ModerationHistoryPanel } from "@/components/owner/moderation-history-panel";
import { BackLink } from "@/components/ui/back-link";
import { PageHeading } from "@/components/ui/page-heading";
import { getAssignedFields, getOwnerFields } from "@/lib/server/fields";
import { requireUser } from "@/lib/server/guards";

const getManagedFieldsForModerationPage = (role: string) =>
  role === "EMPLOYEE" ? getAssignedFields(0, 100) : getOwnerFields(0, 100);

export default async function OwnerModerationHistoryPage({
  searchParams,
}: {
  searchParams: Promise<{ fieldId?: string; page?: string }>;
}) {
  const user = await requireUser();
  const query = await searchParams;
  const fields = await getManagedFieldsForModerationPage(user.userType);
  const allowedFields = fields.content;
  const requestedFieldId = query.fieldId;
  const selectedFieldId = allowedFields.some((field) => field.id === requestedFieldId)
    ? requestedFieldId
    : allowedFields[0]?.id;
  const page = Math.max(0, Number(query.page) || 0);

  return (
    <>
      <BackLink href="/owner" className="mb-5">
        Quan ly san
      </BackLink>
      <PageHeading
        eyebrow="Kiem duyet"
        title="Bao cao vang mat va nhat ky cam"
        description="Theo doi cac bao cao vang mat da ghi nhan va lich su thao tac cam/go cam theo tung san."
      />
      <ModerationHistoryPanel
        key={`${selectedFieldId ?? ""}-${page}`}
        fields={allowedFields}
        selectedFieldId={selectedFieldId ?? ""}
        page={page}
      />
    </>
  );
}
