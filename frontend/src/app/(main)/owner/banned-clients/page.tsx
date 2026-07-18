import { getBannedClients } from "@/lib/server/moderation";

export default async function OwnerBannedClientsPage({
  searchParams,
}: {
  searchParams: Promise<{ fieldId?: string; page?: string }>;
}) {
  const query = await searchParams;
  const fieldId = query.fieldId;
  const data = fieldId ? await getBannedClients(fieldId, Number(query.page) || 0, 20) : null;

  return (
    <section className="mx-auto max-w-6xl px-4 py-8">
      <h1 className="text-2xl font-black text-slate-950">Banned Clients</h1>
      <p className="mt-2 text-sm text-slate-500">Use `?fieldId=` to list clients currently banned from one of your fields.</p>
      <div className="mt-6 grid gap-3">
        {(data?.content ?? []).map((item) => (
          <article key={item.id} className="rounded-lg border border-slate-200 bg-white p-4">
            <div className="font-bold text-slate-900">{item.userId}</div>
            <div className="mt-2 text-sm text-slate-500">Violations: {item.violationCount}</div>
            <div className="text-sm text-slate-500">Ban date: {item.banDate ?? "-"}</div>
            <div className="text-sm text-slate-500">Last violation: {item.lastViolationDate ?? "-"}</div>
          </article>
        ))}
      </div>
    </section>
  );
}
