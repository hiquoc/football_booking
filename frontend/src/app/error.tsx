"use client";

export default function ErrorPage({
  reset,
}: {
  error: Error & { digest?: string };
  reset: () => void;
}) {
  return (
    <main className="grid min-h-dvh place-items-center bg-slate-50 px-5 text-center text-slate-900">
      <div>
        <p className="text-sm font-black uppercase tracking-[0.2em] text-rose-500">
          Đã xảy ra lỗi
        </p>
        <h1 className="mt-4 text-4xl font-black tracking-[-0.04em]">
          Không thể tải trang này.
        </h1>
        <button
          onClick={reset}
          className="inline-flex items-center gap-2 rounded-full bg-green-600 px-6 py-3.5 text-sm font-black text-white"
        >
          Thử lại
        </button>
      </div>
    </main>
  );
}
