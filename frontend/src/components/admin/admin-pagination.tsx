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
          className="inline-flex items-center gap-2 rounded-full bg-slate-500 px-4 py-2.5 text-sm font-bold text-white hover:bg-slate-600 shadow-none"
        >
          <ChevronLeft className="size-4" /> Trang trước
        </Link>
      ) : null}
      <span className="text-sm font-semibold text-slate-500">
        {currentPage + 1}/{totalPages}
      </span>
      {currentPage + 1 < totalPages ? (
        <Link
          href={href(currentPage + 1)}
          className="inline-flex items-center gap-2 rounded-full bg-slate-950 px-4 py-2.5 text-sm font-bold text-white hover:bg-slate-800 shadow-none"
        >
          Trang sau <ChevronRight className="size-4" />
        </Link>
      ) : null}
    </nav>
  );
}
