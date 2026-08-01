import { Goal } from "lucide-react";

export function BrandMark({ compact = false }: { compact?: boolean }) {
  return (
    <span className="inline-flex items-center gap-2.5 font-black tracking-[-0.04em] text-slate-950">
      <span className="grid size-9 place-items-center rounded-xl bg-green-600 text-white shadow-[0_10px_24px_rgba(22,163,74,0.20)]">
        <Goal className="size-5 text-white" strokeWidth={2.4} aria-hidden="true" />
      </span>
      {!compact && <span className="text-xl">PitchUp</span>}
    </span>
  );
}
