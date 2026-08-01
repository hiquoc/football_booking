import Link from "next/link";
import { BackLink } from "@/components/ui/back-link";
import { PageHeading } from "@/components/ui/page-heading";
import { PaymentDisputeStatusButton } from "@/components/ui/payment-dispute-status-button";
import { getAdminPaymentDisputes } from "@/lib/server/moderation";
import type { PaymentDisputeStatus } from "@/lib/api/types";

const statusOptions: Array<{ value?: PaymentDisputeStatus; label: string }> = [
  { label: "Tất cả" },
  { value: "PENDING", label: "Chờ xử lý" },
  { value: "APPROVED", label: "Đã chấp nhận" },
  { value: "REJECTED", label: "Đã từ chối" },
];

export default async function AdminPaymentDisputesPage({
  searchParams,
}: {
  searchParams: Promise<{ status?: PaymentDisputeStatus }>;
}) {
  const query = await searchParams;
  const status = parseStatus(query.status);
  const data = await getAdminPaymentDisputes(0, 20, status);

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

      <nav className="mt-8 flex flex-wrap gap-2" aria-label="Lọc trạng thái tranh chấp">
        {statusOptions.map((option) => {
          const active = option.value === status || (!option.value && !status);
          const href = option.value ? `/admin/payment-disputes?status=${option.value}` : "/admin/payment-disputes";
          return (
            <Link
              key={option.value ?? "all"}
              href={href}
              className={`rounded-xl px-4 py-2 text-sm font-black ${
                active
                  ? "bg-green-600 text-white"
                  : "border border-slate-200 bg-white text-slate-600 hover:border-green-300 hover:text-green-700"
              }`}
            >
              {option.label}
            </Link>
          );
        })}
      </nav>

      {!data.content.length ? (
        <div className="mt-6 rounded-2xl border border-dashed border-slate-300 bg-white p-10 text-center text-sm text-slate-500">
          Chưa có tranh chấp nào phù hợp.
        </div>
      ) : (
        <div className="mt-6 grid gap-4">
          {data.content.map((report) => (
            <article key={report.id} className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
              <div className="flex flex-col justify-between gap-3 sm:flex-row sm:items-start">
                <div>
                  <p className="text-xs font-black uppercase text-slate-400">Mã báo cáo</p>
                  <h2 className="mt-1 break-all text-lg font-black text-slate-950">{report.id}</h2>
                </div>
                <PaymentDisputeStatusButton status={report.status} size="sm" />
              </div>
              <p className="mt-4 text-sm leading-6 text-slate-600">{report.description}</p>
              <div className="mt-4 grid gap-2 border-t border-slate-100 pt-4 text-sm text-slate-500 md:grid-cols-3">
                <Reference label="Lịch đặt" href={`/bookings/${report.bookingId}`} value={report.bookingId} />
                <Reference label="Sân" href={`/fields/${report.fieldId}`} value={report.fieldId} />
                <Reference label="Người bị báo cáo" href={`/users/${report.reportedUserId}/profile`} value={report.reportedUserId} />
              </div>
              {report.adminNote ? (
                <p className="mt-4 rounded-xl bg-slate-50 p-3 text-sm font-semibold text-slate-600">
                  Ghi chú quản trị: {report.adminNote}
                </p>
              ) : null}
            </article>
          ))}
        </div>
      )}
    </>
  );
}

function Reference({ label, href, value }: { label: string; href: string; value: string }) {
  return (
    <span className="min-w-0">
      <span className="font-bold text-slate-500">{label}: </span>
      <Link href={href} className="break-all font-black text-green-700 hover:text-green-800">
        {value}
      </Link>
    </span>
  );
}

function parseStatus(value: PaymentDisputeStatus | undefined) {
  return value === "PENDING" || value === "APPROVED" || value === "REJECTED"
    ? value
    : undefined;
}
