import { AlertCircle, ArrowRight } from "lucide-react";
import Link from "next/link";
import { FieldCard } from "./field-card";
import { getFeaturedFields } from "@/lib/server/fields";
import { getCurrentUser } from "@/lib/server/session";

export async function FeaturedFields() {
  const [page, user] = await Promise.all([
    getFeaturedFields().catch(() => null),
    getCurrentUser(),
  ]);
  const canFavorite = user?.userType === "CLIENT" || user?.userType === "EMPLOYEE";
  if (!page) {
    return (
      <div className="rounded-[2rem] border border-amber-200 bg-amber-50 p-8 text-amber-950">
        <AlertCircle className="size-7" aria-hidden="true" />
        <h3 className="mt-4 text-lg font-black">
          Tạm thời không thể tải danh sách sân
        </h3>
        <p className="mt-2 max-w-xl text-sm leading-6 text-amber-900/75">
          Không thể kết nối đến dịch vụ sân. Vui lòng thử lại sau.
        </p>
      </div>
    );
  }
  if (!page.content.length) return <EmptyFields />;
  return (
    <>
      <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-3">
        {page.content.map((field) => (
          <FieldCard key={field.id} field={field} canFavorite={canFavorite} />
        ))}
      </div>
      <div className="mt-9 text-center">
        <Link
          href="/fields"
          className="inline-flex items-center gap-2 rounded-full border border-slate-200 bg-white px-5 py-3 text-sm font-black text-slate-700 transition hover:border-green-400 hover:text-green-700"
        >
          Xem tất cả sân <ArrowRight className="size-4" />
        </Link>
      </div>
    </>
  );
}

function EmptyFields() {
  return (
    <div className="rounded-[2rem] border border-dashed border-slate-300 bg-white p-10 text-center">
      <h3 className="text-xl font-black text-slate-900">Chưa có sân nào</h3>
      <p className="mt-2 text-slate-500">
        Các sân đã được duyệt sẽ xuất hiện tại đây.
      </p>
      <span className="mt-5 inline-flex items-center gap-2 text-sm font-bold text-green-600">
        Quay lại sau <ArrowRight className="size-4" />
      </span>
    </div>
  );
}
