import { FieldManagementNav } from "@/components/owner/field-management-nav";
import { EmployeeManager } from "@/components/owner/employee-manager";
import { BackLink } from "@/components/ui/back-link";
import { requireUser } from "@/lib/server/guards";

export default async function FieldEmployeesPage({ params }: { params: Promise<{ id: string }> }) {
  const user = await requireUser();
  const { id } = await params;
  return (
    <>
      <BackLink href="/owner/fields" className="mb-5">
        Quay lại danh sách sân
      </BackLink>
      <FieldManagementNav fieldId={id} />
      <EmployeeManager fieldId={id} isOwner={user.userType === "OWNER"} />
    </>
  );
}
