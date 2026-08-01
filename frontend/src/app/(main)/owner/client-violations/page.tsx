import Link from "next/link";
import { ClientBanButton } from "@/components/owner/client-ban-button";
import { BackLink } from "@/components/ui/back-link";
import { PageHeading } from "@/components/ui/page-heading";
import { getAssignedFields, getOwnerFields } from "@/lib/server/fields";
import { requireUser } from "@/lib/server/guards";
import { getFieldViolations } from "@/lib/server/moderation";

const getManagedFieldsForViolationPage = (role: string) =>
  role === "EMPLOYEE" ? getAssignedFields(0, 100) : getOwnerFields(0, 100);

export default async function OwnerClientViolationsPage({
  searchParams,
}: {
  searchParams: Promise<{ fieldId?: string; page?: string }>;
}) {
  const user = await requireUser();
  const query = await searchParams;
  const fields = await getManagedFieldsForViolationPage(user.userType);
  const allowedFields = fields.content;
  const requestedFieldId = query.fieldId;
  const selectedFieldId = allowedFields.some((field) => field.id === requestedFieldId)
    ? requestedFieldId
    : allowedFields[0]?.id;
  const requestedDenied = Boolean(requestedFieldId && requestedFieldId !== selectedFieldId);
  const page = Math.max(0, Number(query.page) || 0);
  const data = selectedFieldId ? await getFieldViolations(selectedFieldId, page, 20) : null;

  return (
    <>
      <BackLink href="/owner" className="mb-5">
        Quản lý sân
      </BackLink>
      <PageHeading
        eyebrow="Vi phạm"
        title="Lượt vắng mặt của khách"
        description="Theo dõi các lượt vắng mặt và cấm khách đặt sân khi cần thiết."
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
      {requestedDenied ? (
        <p className="mt-3 text-sm font-semibold text-amber-700">
          Sân được yêu cầu không thuộc tài khoản của bạn, nên danh sách đã được chuyển về sân hợp lệ.
        </p>
      ) : null}
      <div className="mt-6 overflow-hidden rounded-2xl border border-slate-200 bg-white">
        <table className="w-full text-left text-sm">
          <thead className="bg-slate-50 text-slate-500">
            <tr>
              <th className="px-4 py-3">Khách</th>
              <th className="px-4 py-3">Số lần vi phạm</th>
              <th className="px-4 py-3">Trạng thái cấm</th>
              <th className="px-4 py-3">Vi phạm gần nhất</th>
              <th className="px-4 py-3">Thao tác</th>
            </tr>
          </thead>
          <tbody>
            {(data?.content ?? []).map((item) => (
              <tr key={item.id} className="border-t border-slate-100">
                <td className="px-4 py-3 font-medium text-slate-900">
                  <Link href={`/users/${item.userId}/profile`} className="font-black text-green-700 hover:text-green-800">
                    {item.username ?? item.userDisplayName ?? item.userId}
                  </Link>
                  {item.phoneNumber ? <p className="mt-1 text-xs font-semibold text-slate-500">{item.phoneNumber}</p> : null}
                </td>
                <td className="px-4 py-3">{item.violationCount}</td>
                <td className="px-4 py-3">
                  {item.banned ? (
                    <span className="inline-flex h-8 items-center rounded-lg bg-slate-500 px-3 text-xs font-black text-white">
                      Đã cấm
                    </span>
                  ) : (
                    <span className="inline-flex h-8 items-center rounded-lg bg-green-600 px-3 text-xs font-black text-white">
                      Đang hoạt động
                    </span>
                  )}
                </td>
                <td className="px-4 py-3">{item.lastViolationDate ?? "-"}</td>
                <td className="px-4 py-3">
                  {selectedFieldId ? (
                    <ClientBanButton fieldId={selectedFieldId} userId={item.userId} banned={item.banned} />
                  ) : null}
                </td>
              </tr>
            ))}
            {!data?.content.length ? (
              <tr className="border-t border-slate-100">
                <td className="px-4 py-8 text-center text-slate-500" colSpan={5}>
                  {selectedFieldId ? "Chưa có vi phạm nào cho sân này." : "Tài khoản của bạn chưa có sân để quản lý."}
                </td>
              </tr>
            ) : null}
          </tbody>
        </table>
      </div>
      {data && data.totalPages > 1 && selectedFieldId ? (
        <div className="mt-6 flex items-center justify-center gap-3">
          {page > 0 ? (
            <Link href={pageHref(selectedFieldId, page - 1)} className="rounded-xl border border-slate-200 bg-white px-4 py-2 text-sm font-bold text-slate-700">
              Trước
            </Link>
          ) : null}
          <span className="text-sm font-semibold text-slate-500">
            Trang {data.page + 1}/{Math.max(data.totalPages, 1)}
          </span>
          {!data.last ? (
            <Link href={pageHref(selectedFieldId, page + 1)} className="rounded-xl bg-green-600 px-4 py-2 text-sm font-bold text-white">
              Sau
            </Link>
          ) : null}
        </div>
      ) : null}
    </>
  );
}

function pageHref(fieldId: string, page: number) {
  const params = new URLSearchParams({ fieldId, page: String(page) });
  return `/owner/client-violations?${params}`;
}
