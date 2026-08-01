import Link from "next/link";
import { ChevronLeft, ChevronRight } from "lucide-react";

export function AdminPagination({
  currentPage,
  totalPages,
  pathname,
  params = {},
}: {
  currentPage: number;
  totalPages: number;
  pathname: string;
  params?: Record<string, string>;
}) {
  if (totalPages <= 1) return null;

  function href(page: number) {
    const query = new URLSearchParams({ ...params, page: String(page + 1) });
    return `${pathname}?${query}`;
  }

  return (
    <nav
      aria-label="Phân trang"
      className="mt-8 flex items-center justify-center gap-3"
    >
      {currentPage > 0 ? (
        <Link
          href={href(currentPage - 1)}
          className="inline-flex items-center gap-2 rounded-xl border border-slate-200 bg-white px-4 py-2.5 text-sm font-bold text-slate-700 hover:border-green-300 hover:text-green-700 shadow-none"
        >
          <ChevronLeft className="size-4" /> Trang trước
        </Link>
      ) : null}
      <span className="inline-flex min-h-10 items-center rounded-xl bg-green-50 px-4 text-sm font-black text-green-700">
        {currentPage + 1}/{totalPages}
      </span>
      {currentPage + 1 < totalPages ? (
        <Link
          href={href(currentPage + 1)}
          className="inline-flex items-center gap-2 rounded-xl bg-green-600 px-4 py-2.5 text-sm font-bold text-white hover:bg-green-700 shadow-none"
        >
          Trang sau <ChevronRight className="size-4" />
        </Link>
      ) : null}
    </nav>
  );
}
