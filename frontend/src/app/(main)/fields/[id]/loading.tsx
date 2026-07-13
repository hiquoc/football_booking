export default function FieldDetailLoading() {
  return (
    <div className="mx-auto min-h-[70vh] w-full max-w-[90rem] animate-pulse px-5 py-8 sm:px-8" aria-busy="true">
      <div className="aspect-[16/7] rounded-[2rem] bg-slate-200" />
      <div className="mt-8 grid gap-8 lg:grid-cols-[minmax(0,1fr)_22rem]">
        <div className="space-y-4"><div className="h-12 w-2/3 rounded-xl bg-slate-200" /><div className="h-5 w-1/2 rounded bg-slate-100" /><div className="h-40 rounded-2xl bg-slate-100" /></div>
        <div className="h-72 rounded-[2rem] bg-slate-200" />
      </div>
    </div>
  );
}
