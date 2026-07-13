export default function MainLoading() {
  return (
    <main className="mx-auto w-full max-w-[90rem] px-5 py-10 sm:px-8" aria-busy="true">
      <p className="text-sm font-bold uppercase tracking-[0.18em] text-sky-600">Đặt sân bóng</p>
      <h1 className="mt-2 text-3xl font-black text-slate-950">Đang tải nội dung</h1>
      <p className="mt-2 text-sm text-slate-500">Thông tin cơ bản đã sẵn sàng. Dữ liệu mới nhất đang được tải về.</p>
      <div className="mt-8 grid gap-5 md:grid-cols-2 lg:grid-cols-3">
        {Array.from({ length: 6 }, (_, index) => (
          <div key={index} className="overflow-hidden rounded-[1.5rem] border border-slate-200 bg-white">
            <div className="h-36 animate-pulse bg-slate-200" />
            <div className="space-y-3 p-5">
              <div className="h-5 w-2/3 animate-pulse rounded bg-slate-200" />
              <div className="h-4 w-full animate-pulse rounded bg-slate-100" />
              <div className="h-4 w-1/2 animate-pulse rounded bg-slate-100" />
            </div>
          </div>
        ))}
      </div>
    </main>
  );
}
