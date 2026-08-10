import Link from "next/link";
import { FieldManagementNav } from "@/components/owner/field-management-nav";
import { OwnerReservationsPanel } from "@/components/owner/owner-reservations-panel";
import { BackLink } from "@/components/ui/back-link";
import { PageHeading } from "@/components/ui/page-heading";

export default async function FieldReservationsPage({
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
      <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
        <PageHeading eyebrow="Quản lý sân" title="Lịch giữ sân" />
        <Link
          href={`/fields/${id}`}
          className="action-button w-fit bg-green-600 px-5 text-white hover:bg-green-700"
        >
          Tạo lịch giữ sân
        </Link>
      </div>
      <div className="mt-8">
        <OwnerReservationsPanel fieldId={id} />
      </div>
    </div>
  );
}
