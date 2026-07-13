export default function LoginLoading() {
  return (
    <main className="grid min-h-dvh animate-pulse bg-sky-50 lg:grid-cols-[1.35fr_0.65fr]" aria-busy="true">
      <div className="hidden bg-sky-100 lg:block" />
      <div className="flex items-center justify-center bg-white px-5"><div className="w-full max-w-md space-y-5"><div className="h-10 w-48 rounded bg-slate-200" /><div className="h-16 rounded-2xl bg-slate-100" /><div className="h-14 rounded-2xl bg-slate-200" /></div></div>
    </main>
  );
}
