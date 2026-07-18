import { getAdminPaymentDisputes } from "@/lib/server/moderation";
import type { PaymentDisputeStatus } from "@/lib/api/types";

export default async function AdminPaymentDisputesPage({
  searchParams,
}: {
  searchParams: Promise<{ status?: PaymentDisputeStatus }>;
}) {
  const query = await searchParams;
  const data = await getAdminPaymentDisputes(0, 20, query.status);
  return (
    <section className="mx-auto max-w-6xl px-4 py-8">
      <h1 className="text-2xl font-black text-slate-950">Payment Dispute Review</h1>
      <div className="mt-6 grid gap-3">
        {data.content.map((report) => (
          <article key={report.id} className="rounded-lg border border-slate-200 bg-white p-4">
            <div className="flex items-center justify-between gap-4">
              <strong className="text-slate-950">{report.id}</strong>
              <span className="text-sm font-bold text-slate-600">{report.status}</span>
            </div>
            <p className="mt-2 text-sm text-slate-600">{report.description}</p>
            <div className="mt-3 flex flex-wrap gap-2 text-xs text-slate-500">
              <span>Booking: {report.bookingId}</span>
              <span>Client: {report.reportedUserId}</span>
              <span>Owner: {report.ownerId}</span>
            </div>
          </article>
        ))}
      </div>
    </section>
  );
}
