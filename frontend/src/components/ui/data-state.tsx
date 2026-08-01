import { CircleAlert, Inbox } from "lucide-react";

export function DataError({
  title = "Không thể tải dữ liệu",
  description = "Vui lòng thử lại sau.",
}: {
  title?: string;
  description?: string;
}) {
  return (
    <div className="rounded-2xl border border-amber-200 bg-amber-50 p-7 text-amber-950">
      <CircleAlert className="size-6" />
      <h2 className="mt-3 font-black">{title}</h2>
      <p className="mt-1 text-sm text-amber-800">{description}</p>
    </div>
  );
}

export function DataEmpty({
  title,
  description,
}: {
  title: string;
  description: string;
}) {
  return (
    <div className="rounded-2xl border border-dashed border-slate-300 bg-white p-10 text-center">
      <Inbox className="mx-auto size-8 text-slate-300" />
      <h2 className="mt-4 text-lg font-black text-slate-900">{title}</h2>
      <p className="mt-2 text-sm text-slate-500">{description}</p>
    </div>
  );
}

export function ListSkeleton({ count = 3 }: { count?: number }) {
  return (
    <div className="space-y-4">
      {Array.from({ length: count }, (_, index) => (
        <div key={index} className="h-32 animate-pulse rounded-2xl bg-slate-200" />
      ))}
    </div>
  );
}

export function DetailSkeleton() {
  return (
    <div className="animate-pulse" aria-busy="true" aria-label="Đang tải nội dung">
      <div className="mb-5 h-5 w-32 rounded bg-slate-200" />
      <div className="grid gap-7 lg:grid-cols-[minmax(0,1fr)_22rem]">
        <div className="overflow-hidden rounded-2xl border border-slate-200 bg-white">
          <div className="h-44 bg-slate-200" />
          <div className="space-y-4 p-6 sm:p-8">
            <div className="h-8 w-2/3 rounded bg-slate-200" />
            <div className="h-5 w-1/2 rounded bg-slate-100" />
            <div className="grid gap-4 pt-4 sm:grid-cols-2">
              <div className="h-24 rounded-2xl bg-slate-100" />
              <div className="h-24 rounded-2xl bg-slate-100" />
            </div>
          </div>
        </div>
        <div className="h-72 rounded-2xl bg-slate-200" />
      </div>
    </div>
  );
}

export function FormSkeleton() {
  return (
    <div className="grid animate-pulse gap-7 lg:grid-cols-[minmax(0,1fr)_22rem]" aria-busy="true" aria-label="Đang tải biểu mẫu">
      <div className="space-y-6 rounded-2xl border border-slate-200 bg-white p-6 sm:p-8">
        <div className="h-8 w-1/2 rounded bg-slate-200" />
        {[0, 1, 2, 3].map((item) => (
          <div key={item} className="h-24 rounded-2xl bg-slate-100" />
        ))}
      </div>
      <div className="h-80 rounded-2xl bg-slate-200" />
    </div>
  );
}

export function ProfileSkeleton() {
  return (
    <div className="animate-pulse space-y-7" aria-busy="true" aria-label="Đang tải hồ sơ">
      <section className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
        <div className="h-56 bg-slate-200 sm:h-72 lg:h-80" />
        <div className="px-5 pb-6 sm:px-8 sm:pb-8">
          <div className="-mt-16 flex flex-col gap-4 sm:-mt-20 sm:flex-row sm:items-end sm:justify-between">
            <div className="flex flex-col gap-4 sm:flex-row sm:items-end">
              <div className="size-32 rounded-3xl bg-slate-200 ring-4 ring-white sm:size-40" />
              <div className="pb-3">
                <div className="h-8 w-56 rounded-lg bg-slate-200" />
                <div className="mt-3 flex gap-2">
                  <div className="h-8 w-24 rounded-full bg-slate-100" />
                  <div className="h-8 w-32 rounded-full bg-slate-100" />
                </div>
              </div>
            </div>
            <div className="h-12 w-40 rounded-full bg-slate-100" />
          </div>
          <div className="mt-7 max-w-3xl rounded-2xl border border-slate-100 bg-slate-50 p-5">
            <div className="h-4 w-24 rounded-full bg-slate-200" />
            <div className="mt-3 h-4 w-full rounded-full bg-slate-200" />
            <div className="mt-2 h-4 w-2/3 rounded-full bg-slate-200" />
          </div>
        </div>
      </section>

      <section className="grid gap-5 lg:grid-cols-[minmax(0,1.35fr)_minmax(22rem,0.65fr)]">
        <div className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
          <div className="h-7 w-48 rounded-lg bg-slate-200" />
          <div className="mt-6 grid gap-4 sm:grid-cols-3">
            <div className="h-24 rounded-xl bg-slate-100" />
            <div className="h-24 rounded-xl bg-slate-100" />
            <div className="h-24 rounded-xl bg-slate-100" />
          </div>
          <div className="mt-6 space-y-4">
            <div className="h-10 rounded-xl bg-slate-100" />
            <div className="h-10 rounded-xl bg-slate-100" />
            <div className="h-10 rounded-xl bg-slate-100" />
          </div>
        </div>
        <div className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
          <div className="h-7 w-32 rounded-lg bg-slate-200" />
          <div className="mt-6 space-y-5">
            <div className="h-12 rounded-xl bg-slate-100" />
            <div className="h-12 rounded-xl bg-slate-100" />
            <div className="h-12 rounded-xl bg-slate-100" />
          </div>
        </div>
      </section>

      <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
        <div className="flex items-center justify-between gap-3">
          <div className="h-7 w-40 rounded-lg bg-slate-200" />
          <div className="h-5 w-28 rounded-full bg-slate-100" />
        </div>
        <div className="mt-5 grid gap-4 md:grid-cols-2">
          <div className="h-36 rounded-xl bg-slate-100" />
          <div className="h-36 rounded-xl bg-slate-100" />
        </div>
      </section>
    </div>
  );
}
