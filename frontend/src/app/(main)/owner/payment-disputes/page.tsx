import { getOwnerPaymentDisputes } from "@/lib/server/moderation";

export default async function OwnerPaymentDisputesPage() {
  const data = await getOwnerPaymentDisputes(0, 20);
  return (
    <section className="mx-auto max-w-6xl px-4 py-8">
      <h1 className="text-2xl font-black text-slate-950">Payment Dispute Reports</h1>
      <div className="mt-6 grid gap-3">
        {data.content.map((report) => (
          <article key={report.id} className="rounded-lg border border-slate-200 bg-white p-4">
            <div className="flex items-center justify-between gap-4">
              <strong className="text-slate-950">{report.bookingId}</strong>
              <span className="text-sm font-bold text-slate-600">{report.status}</span>
            </div>
            <p className="mt-2 text-sm text-slate-600">{report.description}</p>
            <p className="mt-2 text-xs text-slate-500">Evidence: {report.imageUrls.length} image(s)</p>
          </article>
        ))}
      </div>
    </section>
  );
}
