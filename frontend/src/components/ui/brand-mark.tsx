import { Goal } from "lucide-react";

export function BrandMark({ compact = false }: { compact?: boolean }) {
  return (
    <span className="inline-flex items-center gap-2.5 font-black tracking-[-0.04em] text-slate-950">
      <span className="grid size-9 place-items-center rounded-xl bg-sky-500 text-slate-950 ">
        <Goal className="size-5 text-white" strokeWidth={2.4} aria-hidden="true" />
      </span>
      {!compact && <span className="text-xl">PitchUp</span>}
    </span>
  );
}
