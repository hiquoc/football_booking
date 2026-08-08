import Link from "next/link";
import { BackLink } from "@/components/ui/back-link";
import { PageHeading } from "@/components/ui/page-heading";
import { PaymentDisputeStatusButton } from "@/components/ui/payment-dispute-status-button";
import { getOwnerPaymentDisputes } from "@/lib/server/moderation";

export default async function OwnerPaymentDisputesPage() {
  const data = await getOwnerPaymentDisputes(0, 20);

  return (
    <>
      <BackLink href="/owner" className="mb-5">
        Quản lý sân
      </BackLink>
      <PageHeading
        eyebrow="Thanh toán"
        title="Tranh chấp thanh toán"
        description="Theo dõi các báo cáo tranh chấp bạn đã gửi cho đội ngũ quản trị."
      />
      {!data.content.length ? (
        <div className="mt-6 rounded-2xl border border-dashed border-slate-300 bg-white p-10 text-center text-sm text-slate-500">
          Chưa có tranh chấp thanh toán nào.
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

                <div>
                  <span className="font-bold text-slate-500">Lịch đặt: </span>
                  <span className="break-all">
                    {report.bookingId}
                  </span>
                   </div>
                <Reference label="Sân" href={`/fields/${report.fieldId}`} value={report.fieldId} />
                <UserReference
                  label="Người bị báo cáo"
                  href={`/users/${report.reportedUserId}/profile`}
                  name={report.reportedUsername ?? report.reportedUserId}
                  phone={report.reportedPhoneNumber}
                />
              </div>
              {/* <p className="mt-4 text-xs font-bold text-slate-500">
                Bằng chứng: {report.imageUrls.length} ảnh
              </p> */}
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

function UserReference({ label, href, name, phone }: { label: string; href: string; name: string; phone?: string | null }) {
  return (
    <span className="min-w-0">
      <span className="font-bold text-slate-500">{label}: </span>
      <Link href={href} className="break-all font-black text-green-700 hover:text-green-800">
        {name}
      </Link>
      {phone ? <span className="mt-1 block font-semibold text-slate-500">{phone}</span> : null}
    </span>
  );
}
