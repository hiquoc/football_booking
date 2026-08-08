import { PaymentDisputesPanel } from "@/components/admin/payment-disputes-panel";
import { BackLink } from "@/components/ui/back-link";
import { PageHeading } from "@/components/ui/page-heading";
import type { PaymentDisputeStatus } from "@/lib/api/types";

export default async function AdminPaymentDisputesPage({
  searchParams,
}: {
  searchParams: Promise<{ status?: PaymentDisputeStatus; fieldIds?: string; fieldId?: string; page?: string }>;
}) {
  const query = await searchParams;
  const status = parseStatus(query.status);
  const fieldIds = parseFieldIds(query.fieldIds ?? query.fieldId);
  const page = Math.max(0, (Number(query.page) || 1) - 1);

  return (
    <>
      <BackLink href="/admin" className="mb-5">
        Bảng điều khiển
      </BackLink>
      <PageHeading
        eyebrow="Kiểm duyệt thanh toán"
        title="Tranh chấp thanh toán"
        description="Theo dõi và xử lý các báo cáo tranh chấp do chủ sân gửi lên."
      />
      <PaymentDisputesPanel
        page={page}
        status={status}
        fieldIds={fieldIds}
      />
    </>
  );
}

function parseStatus(value: PaymentDisputeStatus | undefined) {
  return value === "PENDING" || value === "APPROVED" || value === "REJECTED" || value === "" ? value : "PENDING";
}

function parseFieldIds(value: string | undefined) {
  return (value ?? "")
    .split(",")
    .map((fieldId) => fieldId.trim())
    .filter(Boolean);
}
