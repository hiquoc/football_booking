import Link from "next/link";
import { BackLink } from "@/components/ui/back-link";
import { PageHeading } from "@/components/ui/page-heading";
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
    <>
      <BackLink href="/owner" className="mb-5">
        Quản lý sân
      </BackLink>
      <PageHeading
        eyebrow="Vi phạm"
        title="Khách đang bị cấm"
        description="Chọn một sân bằng tham số fieldId để xem danh sách khách đang bị cấm đặt sân."
      />
      {!fieldId ? (
        <p className="mt-6 rounded-2xl border border-dashed border-slate-300 bg-white p-8 text-center text-sm font-semibold text-slate-500">
          Thêm `?fieldId=` vào đường dẫn để xem danh sách khách bị cấm của một sân.
        </p>
      ) : (
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
                <span className="inline-flex h-8 w-fit items-center rounded-lg bg-slate-500 px-3 text-xs font-black text-white">
                  Đã cấm
                </span>
              </div>
              <div className="mt-4 grid gap-2 text-sm text-slate-500 sm:grid-cols-3">
                <span>Số lần vi phạm: <strong className="text-slate-900">{item.violationCount}</strong></span>
                <span>Ngày cấm: <strong className="text-slate-900">{item.banDate ?? "-"}</strong></span>
                <span>Vi phạm gần nhất: <strong className="text-slate-900">{item.lastViolationDate ?? "-"}</strong></span>
              </div>
            </article>
          ))}
          {fieldId && !data?.content.length ? (
            <p className="rounded-2xl border border-dashed border-slate-300 bg-white p-8 text-center text-sm font-semibold text-slate-500">
              Sân này chưa có khách nào bị cấm.
            </p>
          ) : null}
        </div>
      )}
    </>
  );
}
