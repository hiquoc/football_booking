import { skillLabel } from "@/components/community/community-labels";

export function SkillLevelButton({
  value,
  size = "md",
  className = "",
}: {
  value: string | null | undefined;
  size?: "sm" | "md";
  className?: string;
}) {
  const sizeClassName = size === "sm"
    ? "h-8 rounded-lg px-3 text-xs leading-none"
    : "h-10 rounded-lg px-4 text-sm leading-none";

  return (
    <span className={`inline-flex items-center justify-center bg-amber-500 font-black text-white shadow-sm ${sizeClassName} ${className}`}>
      {skillLabel(value)}
    </span>
  );
}
