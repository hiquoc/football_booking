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
