import Link from "next/link";

export default function NotFound() {
  return (
    <main className="grid min-h-dvh place-items-center bg-slate-50 px-5 text-center text-slate-900">
      <div>
        <p className="text-sm font-black uppercase tracking-[0.2em] text-green-700">
          404 · Việt vị
        </p>
        <p className="mt-5 text-slate-500">Trang bạn tìm kiếm không tồn tại.</p>
        <h1 className="mt-4 text-5xl font-black tracking-[-0.05em]">
          Trang này không tồn tại.
        </h1>
        <Link
          href="/"
          className="mt-8 inline-flex items-center gap-2 rounded-full bg-green-600 px-6 py-3.5 text-sm font-black text-white"
        >
          Về trang chủ
        </Link>
      </div>
    </main>
  );
}
