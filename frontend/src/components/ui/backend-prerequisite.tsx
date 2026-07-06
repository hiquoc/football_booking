import { Construction } from "lucide-react";
export function BackendPrerequisite({
  title,
  description,
}: {
  title: string;
  description: string;
}) {
  return (
    <div className="rounded-[2rem] border border-amber-200 bg-amber-50 p-8 text-amber-950">
      <span className="grid size-12 place-items-center rounded-2xl bg-amber-200/70">
        <Construction className="size-6" />
      </span>
      <h2 className="mt-5 text-2xl font-black">{title}</h2>
      <p className="mt-3 max-w-2xl text-sm leading-6 text-amber-800">
        {description}
      </p>
    </div>
  );
}
