import Link from "next/link";
import { ShieldX } from "lucide-react";

export function AccessDenied() {
  return (
    <section className="mx-auto flex w-full max-w-3xl flex-1 items-center justify-center px-5 py-12 text-center sm:px-8">
      <div className="w-full rounded-[2rem] border border-slate-200 bg-white px-6 py-12 shadow-sm sm:px-12 sm:py-16">
        <span className="mx-auto grid size-16 place-items-center rounded-2xl bg-rose-100 text-rose-600">
          <ShieldX className="size-8" aria-hidden="true" />
        </span>
        <h1 className="mt-5 text-3xl font-black text-slate-950">
          Bạn không có quyền truy cập
        </h1>
        <p className="mt-3 text-slate-500">
          Tài khoản hiện tại không được phép thực hiện hành động này.
        </p>
        <Link
          href="/"
          className="mt-7 inline-flex rounded-full bg-slate-950 px-5 py-3 text-sm font-black text-white"
        >
          Về trang chủ
        </Link>
      </div>
    </section>
  );
}