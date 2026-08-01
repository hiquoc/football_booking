import { BackLink } from "@/components/ui/back-link";

export function CommunityDetailSkeleton() {
  return (
    <div className="mx-auto w-full max-w-[90rem] px-5 py-10 sm:px-8">
      <BackLink href="/community" className="mb-5">Quay lại cộng đồng</BackLink>
      <div className="grid gap-7 lg:grid-cols-[minmax(0,1.35fr)_minmax(24rem,0.85fr)]">
        <main className="space-y-5">
          <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
            <div className="flex gap-2">
              <Skeleton className="h-10 w-28 rounded-lg" />
              <Skeleton className="h-10 w-24 rounded-lg" />
              <Skeleton className="h-10 w-24 rounded-lg" />
            </div>
            <Skeleton className="mt-5 h-10 w-3/4 rounded-xl" />
            <div className="mt-5 space-y-3">
              <Skeleton className="h-4 w-full rounded-full" />
              <Skeleton className="h-4 w-11/12 rounded-full" />
              <Skeleton className="h-4 w-2/3 rounded-full" />
            </div>
          </section>

          <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
            <Skeleton className="h-6 w-44 rounded-lg" />
            <div className="mt-5 grid gap-4 sm:grid-cols-2">
              {Array.from({ length: 6 }, (_, index) => (
                <div key={index} className="rounded-xl border border-slate-200 bg-slate-50 p-4">
                  <Skeleton className="h-4 w-24 rounded-full" />
                  <Skeleton className="mt-3 h-5 w-36 rounded-full" />
                </div>
              ))}
            </div>
          </section>

          <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
            <div className="flex items-center justify-between gap-3">
              <Skeleton className="h-6 w-48 rounded-lg" />
              <Skeleton className="h-10 w-20 rounded-xl" />
            </div>
            <div className="mt-5 grid gap-4 sm:grid-cols-2">
              {Array.from({ length: 4 }, (_, index) => (
                <div key={index} className="rounded-2xl border border-slate-200 bg-slate-50 p-4">
                  <div className="flex items-center justify-between">
                    <Skeleton className="h-4 w-28 rounded-full" />
                    <Skeleton className="h-5 w-12 rounded-full" />
                  </div>
                  <Skeleton className="mt-4 h-2.5 w-full rounded-full" />
                </div>
              ))}
            </div>
          </section>
        </main>

        <aside className="space-y-4">
          <section className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
            <Skeleton className="h-4 w-24 rounded-full" />
            <div className="mt-4 grid gap-3">
              <Skeleton className="h-12 w-full rounded-full" />
              <Skeleton className="h-12 w-full rounded-full" />
            </div>
          </section>
          <section className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
            <Skeleton className="h-6 w-40 rounded-lg" />
            <div className="mt-4 space-y-3">
              <Skeleton className="h-20 w-full rounded-2xl" />
              <Skeleton className="h-20 w-full rounded-2xl" />
            </div>
          </section>
        </aside>
      </div>
    </div>
  );
}

function Skeleton({ className }: { className: string }) {
  return <div className={`animate-pulse bg-slate-200 ${className}`} />;
}
