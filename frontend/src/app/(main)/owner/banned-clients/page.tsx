import Link from "next/link";
import { ClientBanButton } from "@/components/owner/client-ban-button";
import { BackLink } from "@/components/ui/back-link";
import { PageHeading } from "@/components/ui/page-heading";
import { getAssignedFields, getOwnerFields } from "@/lib/server/fields";
import { requireUser } from "@/lib/server/guards";
import { getBannedClients } from "@/lib/server/moderation";

const getManagedFieldsForBannedClientsPage = (role: string) =>
  role === "EMPLOYEE" ? getAssignedFields(0, 100) : getOwnerFields(0, 100);

export default async function OwnerBannedClientsPage({
  searchParams,
}: {
  searchParams: Promise<{ fieldId?: string; page?: string }>;
}) {
  const user = await requireUser();
  const query = await searchParams;
  const fields = await getManagedFieldsForBannedClientsPage(user.userType);
  const allowedFields = fields.content;
  const requestedFieldId = query.fieldId;
  const selectedFieldId = allowedFields.some((field) => field.id === requestedFieldId)
    ? requestedFieldId
    : allowedFields[0]?.id;
  const page = Math.max(0, Number(query.page) || 0);
  const data = selectedFieldId ? await getBannedClients(selectedFieldId, page, 20) : null;

  return (
    <>
      <BackLink href="/owner" className="mb-5">
        Quản lý sân
      </BackLink>
      <PageHeading
        eyebrow="Vi phạm"
        title="Khách đang bị cấm"
        description="Xem danh sách khách đang bị cấm đặt sân và gỡ cấm khi cần."
      />

      <form className="mt-6 flex flex-col gap-3 rounded-2xl border border-slate-200 bg-white p-4 sm:flex-row sm:items-end">
        <label className="flex-1 text-sm font-semibold text-slate-700">
          Sân
          <select
            name="fieldId"
            defaultValue={selectedFieldId ?? ""}
            className="mt-2 w-full rounded-xl border border-slate-200 bg-white px-3 py-2 text-sm text-slate-900 outline-none focus:border-green-500 focus:ring-4 focus:ring-green-100"
            disabled={!allowedFields.length}
          >
            {!allowedFields.length ? <option value="">Chưa có sân quản lý</option> : null}
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
          className="inline-flex justify-center rounded-xl bg-green-600 px-4 py-2 text-sm font-bold text-white disabled:cursor-not-allowed disabled:opacity-60"
        >
          Áp dụng
        </button>
      </form>

      <div className="mt-6 grid gap-3">
        {(data?.content ?? []).map((item) => (
          <article key={item.id} className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
            <div className="flex flex-col justify-between gap-3 sm:flex-row sm:items-start">
              <div>
                <Link
                  href={`/users/${item.userId}/profile`}
                  className="font-black text-green-700 hover:text-green-800"
                >
                  {item.username ?? item.userDisplayName ?? item.userId}
                </Link>
                {item.phoneNumber ? <p className="mt-1 text-sm font-semibold text-slate-500">{item.phoneNumber}</p> : null}
              </div>
              <div className="flex items-center gap-2">
                {selectedFieldId ? <ClientBanButton fieldId={selectedFieldId} userId={item.userId} banned={item.banned} showStatus /> : null}
              </div>
            </div>
            <div className="mt-4 grid gap-2 text-sm text-slate-500 sm:grid-cols-3">
              <span>Số lần vi phạm: <strong className="text-slate-900">{item.violationCount}</strong></span>
              <span>Ngày cấm: <strong className="text-slate-900">{item.banDate ? formatDateTime(item.banDate) : "-"}</strong></span>
              <span>Vi phạm gần nhất: <strong className="text-slate-900">{item.lastViolationDate ? formatDateTime(item.lastViolationDate) : "-"}</strong></span>
            </div>
          </article>
        ))}
        {!data?.content.length ? (
          <p className="rounded-2xl border border-dashed border-slate-300 bg-white p-8 text-center text-sm font-semibold text-slate-500">
            {selectedFieldId ? "Sân này chưa có khách nào bị cấm." : "Tài khoản của bạn chưa có sân để quản lý."}
          </p>
        ) : null}
      </div>
    </>
  );
}

function formatDateTime(value: string) {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return new Intl.DateTimeFormat("vi-VN", {
    dateStyle: "short",
    timeStyle: "short",
  }).format(date);
}
