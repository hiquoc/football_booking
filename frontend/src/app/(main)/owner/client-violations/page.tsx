import { getFieldViolations } from "@/lib/server/moderation";
import { getAssignedFields, getOwnerFields } from "@/lib/server/fields";
import { requireUser } from "@/lib/server/guards";
import { ClientBanButton } from "@/components/owner/client-ban-button";
import Link from "next/link";

export default async function OwnerClientViolationsPage({
  searchParams,
}: {
  searchParams: Promise<{ fieldId?: string; page?: string }>;
}) {
  const user = await requireUser();
  const query = await searchParams;
  const fields = user.userType === "EMPLOYEE"
    ? await getAssignedFields(0, 100)
    : await getOwnerFields(0, 100);
  const allowedFields = fields.content;
  const requestedFieldId = query.fieldId;
  const selectedFieldId = allowedFields.some((field) => field.id === requestedFieldId)
    ? requestedFieldId
    : allowedFields[0]?.id;
  const requestedDenied = Boolean(requestedFieldId && requestedFieldId !== selectedFieldId);
  const page = Math.max(0, Number(query.page) || 0);
  const data = selectedFieldId ? await getFieldViolations(selectedFieldId, page, 20) : null;

  return (
    <section className="mx-auto max-w-6xl px-4 py-8">
      <h1 className="text-2xl font-black text-slate-950">Client Violations</h1>
      <p className="mt-2 text-sm text-slate-500">Review no-show violations for fields you are allowed to manage.</p>
      <form className="mt-6 flex flex-col gap-3 rounded-lg border border-slate-200 bg-white p-4 sm:flex-row sm:items-end">
        <label className="flex-1 text-sm font-semibold text-slate-700">
          Field
          <select
            name="fieldId"
            defaultValue={selectedFieldId ?? ""}
            className="mt-2 w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm text-slate-900 outline-none focus:border-emerald-500 focus:ring-2 focus:ring-emerald-100"
            disabled={!allowedFields.length}
          >
            {!allowedFields.length ? <option value="">No managed fields</option> : null}
            {allowedFields.map((field) => (
              <option key={field.id} value={field.id}>
                {field.name}
              </option>
            ))}
          </select>
        </label>
        <button
          type="submit"
          disabled={!allowedFields.length}
          className="inline-flex justify-center rounded-lg bg-slate-950 px-4 py-2 text-sm font-bold text-white hover:bg-slate-800 disabled:cursor-not-allowed disabled:opacity-60"
        >
          Apply
        </button>
      </form>
      {requestedDenied ? (
        <p className="mt-3 text-sm font-semibold text-amber-700">
          The requested field is not available for your account, so the list was filtered to an allowed field.
        </p>
      ) : null}
      <div className="mt-6 overflow-hidden rounded-lg border border-slate-200 bg-white">
        <table className="w-full text-left text-sm">
          <thead className="bg-slate-50 text-slate-500">
            <tr>
              <th className="px-4 py-3">Client</th>
              <th className="px-4 py-3">Violations</th>
              <th className="px-4 py-3">Banned</th>
              <th className="px-4 py-3">Last violation</th>
              <th className="px-4 py-3">Action</th>
            </tr>
          </thead>
          <tbody>
            {(data?.content ?? []).map((item) => (
              <tr key={item.id} className="border-t border-slate-100">
                <td className="px-4 py-3 font-medium text-slate-900">
                  <Link href={`/users/${item.userId}/profile`} className="text-emerald-700 hover:underline">
                    {item.userDisplayName ?? item.userId}
                  </Link>
                </td>
                <td className="px-4 py-3">{item.violationCount}</td>
                <td className="px-4 py-3">{item.banned ? "Yes" : "No"}</td>
                <td className="px-4 py-3">{item.lastViolationDate ?? "-"}</td>
                <td className="px-4 py-3">
                  {selectedFieldId ? <ClientBanButton fieldId={selectedFieldId} userId={item.userId} banned={item.banned} /> : null}
                </td>
              </tr>
            ))}
            {!data?.content.length ? (
              <tr className="border-t border-slate-100">
                <td className="px-4 py-8 text-center text-slate-500" colSpan={5}>
                  {selectedFieldId ? "No client violations found for this field." : "No fields are available for your account."}
                </td>
              </tr>
            ) : null}
          </tbody>
        </table>
      </div>
      {data && data.totalPages > 1 && selectedFieldId ? (
        <div className="mt-6 flex items-center justify-center gap-3">
          {page > 0 ? (
            <Link href={pageHref(selectedFieldId, page - 1)} className="rounded-lg border px-4 py-2 text-sm font-bold">
              Previous
            </Link>
          ) : null}
          <span className="text-sm font-semibold text-slate-500">
            Page {data.page + 1}/{Math.max(data.totalPages, 1)}
          </span>
          {!data.last ? (
            <Link href={pageHref(selectedFieldId, page + 1)} className="rounded-lg bg-slate-950 px-4 py-2 text-sm font-bold text-white">
              Next
            </Link>
          ) : null}
        </div>
      ) : null}
    </section>
  );
}

function pageHref(fieldId: string, page: number) {
  const params = new URLSearchParams({ fieldId, page: String(page) });
  return `/owner/client-violations?${params}`;
}
