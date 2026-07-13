import { FormSkeleton } from "@/components/ui/data-state";

export default function FieldManagementLoading() {
  return (
    <div className="w-full animate-pulse py-2" aria-busy="true">
      <div className="mb-5 h-5 w-32 rounded bg-slate-200" />
      <div className="mb-8 h-12 rounded-2xl bg-slate-100" />
      <div className="mb-8 h-9 w-64 rounded bg-slate-200" />
      <FormSkeleton />
    </div>
  );
}
