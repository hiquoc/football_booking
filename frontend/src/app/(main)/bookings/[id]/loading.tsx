import { DetailSkeleton } from "@/components/ui/data-state";

export default function BookingDetailLoading() {
  return (
    <div className="min-h-[70vh] bg-slate-50">
      <div className="mx-auto w-full max-w-[90rem] px-5 py-10 sm:px-8 sm:py-12">
        <DetailSkeleton />
      </div>
    </div>
  );
}
