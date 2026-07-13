import { CircleAlert, Inbox } from "lucide-react";

export function DataError({
  title = "Không thể tải dữ liệu",
  description = "Vui lòng thử lại sau.",
}: {
  title?: string;
  description?: string;
}) {
  return (
    <div className="rounded-[1.5rem] border border-amber-200 bg-amber-50 p-7 text-amber-950">
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
    <div className="rounded-[1.5rem] border border-dashed border-slate-300 bg-white p-10 text-center">
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
        <div
          key={index}
          className="h-32 animate-pulse rounded-[1.5rem] bg-slate-200"
        />
      ))}
    </div>
  );
}

export function DetailSkeleton() {
  return (
    <div className="animate-pulse" aria-busy="true" aria-label="Đang tải nội dung">
      <div className="mb-5 h-5 w-32 rounded bg-slate-200" />
      <div className="grid gap-7 lg:grid-cols-[minmax(0,1fr)_22rem]">
        <div className="overflow-hidden rounded-[2rem] border border-slate-200 bg-white">
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
        <div className="h-72 rounded-[2rem] bg-slate-200" />
      </div>
    </div>
  );
}

export function FormSkeleton() {
  return (
    <div className="grid animate-pulse gap-7 lg:grid-cols-[minmax(0,1fr)_22rem]" aria-busy="true" aria-label="Đang tải biểu mẫu">
      <div className="space-y-6 rounded-[2rem] border border-slate-200 bg-white p-6 sm:p-8">
        <div className="h-8 w-1/2 rounded bg-slate-200" />
        {[0, 1, 2, 3].map((item) => (
          <div key={item} className="h-24 rounded-2xl bg-slate-100" />
        ))}
      </div>
      <div className="h-80 rounded-[2rem] bg-slate-200" />
    </div>
  );
}

export function ProfileSkeleton() {
  return (
    <div className="grid animate-pulse gap-7 lg:grid-cols-[19rem_minmax(0,1fr)]" aria-busy="true" aria-label="Đang tải hồ sơ">
      <div className="h-72 rounded-[2rem] bg-slate-200" />
      <div className="space-y-5 rounded-[2rem] border border-slate-200 bg-white p-6 sm:p-8">
        <div className="h-8 w-48 rounded bg-slate-200" />
        <div className="h-16 rounded-2xl bg-slate-100" />
        <div className="h-16 rounded-2xl bg-slate-100" />
        <div className="h-28 rounded-2xl bg-slate-100" />
      </div>
    </div>
  );
}
