import { getFieldViolations } from "@/lib/server/moderation";

export default async function OwnerClientViolationsPage({
  searchParams,
}: {
  searchParams: Promise<{ fieldId?: string; page?: string }>;
}) {
  const query = await searchParams;
  const fieldId = query.fieldId;
  const data = fieldId ? await getFieldViolations(fieldId, Number(query.page) || 0, 20) : null;

  return (
    <section className="mx-auto max-w-6xl px-4 py-8">
      <h1 className="text-2xl font-black text-slate-950">Client Violations</h1>
      <p className="mt-2 text-sm text-slate-500">Use `?fieldId=` to review no-show violations for a field you own.</p>
      <div className="mt-6 overflow-hidden rounded-lg border border-slate-200 bg-white">
        <table className="w-full text-left text-sm">
          <thead className="bg-slate-50 text-slate-500">
            <tr>
              <th className="px-4 py-3">Client</th>
              <th className="px-4 py-3">Violations</th>
              <th className="px-4 py-3">Banned</th>
              <th className="px-4 py-3">Last violation</th>
            </tr>
          </thead>
          <tbody>
            {(data?.content ?? []).map((item) => (
              <tr key={item.id} className="border-t border-slate-100">
                <td className="px-4 py-3 font-medium text-slate-900">{item.userId}</td>
                <td className="px-4 py-3">{item.violationCount}</td>
                <td className="px-4 py-3">{item.banned ? "Yes" : "No"}</td>
                <td className="px-4 py-3">{item.lastViolationDate ?? "-"}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </section>
  );
}
