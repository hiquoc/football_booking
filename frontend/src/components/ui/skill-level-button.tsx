import { skillLabel } from "@/components/community/community-labels";

const levelTone: Record<string, { badge: string; dot: string }> = {
  VERY_WEAK: { badge: "border-rose-200 bg-white text-rose-700", dot: "bg-rose-500" },
  WEAK: { badge: "border-orange-200 bg-white text-orange-700", dot: "bg-orange-500" },
  AVERAGE: { badge: "border-amber-200 bg-white text-amber-700", dot: "bg-amber-500" },
  ABOVE_AVERAGE: { badge: "border-lime-200 bg-white text-lime-700", dot: "bg-lime-500" },
  GOOD: { badge: "border-green-200 bg-white text-green-700", dot: "bg-green-500" },
  VERY_GOOD: { badge: "border-emerald-200 bg-white text-emerald-700", dot: "bg-emerald-500" },
  SEMI_PRO: { badge: "border-sky-200 bg-white text-sky-700", dot: "bg-sky-500" },
  PRO: { badge: "border-violet-200 bg-white text-violet-700", dot: "bg-violet-500" },
};

const fallbackTone = { badge: "border-slate-200 bg-white text-slate-600", dot: "bg-slate-400" };

export function SkillLevelButton({
  value,
  size = "md",
  className = "",
}: {
  value: string | null | undefined;
  size?: "md" | "sm";
  className?: string;
}) {
  const sizeClassName = size === "sm"
    ? "h-8 rounded-md px-2 text-[11px] leading-none"
    : "h-10 rounded-md px-2.5 text-xs leading-none";
  const dotClassName = size === "sm" ? "size-1.5" : "size-2";
  const tone = value ? levelTone[value] ?? fallbackTone : fallbackTone;

  return (
    <span className={`inline-flex items-center justify-center gap-1.5 border font-bold ${sizeClassName} ${tone.badge} ${className}`}>
      <span className={`shrink-0 rounded-full ${dotClassName} ${tone.dot}`} />
      {skillLabel(value)}
    </span>
  );
}
