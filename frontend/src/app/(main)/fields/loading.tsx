export default function FieldsLoading() {
  return (
    <main className="mx-auto min-h-[70vh] w-full max-w-[90rem] px-5 py-10 sm:px-8">
      <div className="h-8 w-52 animate-pulse rounded-lg bg-slate-200" />
      <div className="mt-3 h-5 w-full max-w-xl animate-pulse rounded bg-slate-100" />
      <div className="mt-8 rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
        <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
          {[0, 1, 2, 3].map((item) => (
            <div key={item} className="h-16 animate-pulse rounded-xl bg-slate-100" />
          ))}
        </div>
        <div className="mt-4 h-11 animate-pulse rounded-xl bg-slate-100" />
      </div>
      <div className="mt-10 grid gap-6 md:grid-cols-2 lg:grid-cols-3">
        {[0, 1, 2, 3, 4, 5].map((item) => (
          <div key={item} className="h-96 animate-pulse rounded-[1.75rem] bg-slate-200" />
        ))}
      </div>
    </main>
  );
}
