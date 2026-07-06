import { Clock3 } from "lucide-react";
import type { OperatingHours as OperatingHoursType } from "@/lib/api/types";
import { formatDay, formatTime } from "@/lib/field-format";

const order = [
  "MONDAY",
  "TUESDAY",
  "WEDNESDAY",
  "THURSDAY",
  "FRIDAY",
  "SATURDAY",
  "SUNDAY",
] as const;

function dayOrder(day: string) {
  const index = order.indexOf(day as (typeof order)[number]);
  return index === -1 ? order.length : index;
}

export function OperatingHours({
  hours,
}: {
  hours: OperatingHoursType[] | null;
}) {
  const sortedHours = [...(hours ?? [])].sort(
    (a, b) => dayOrder(a.dayOfWeek) - dayOrder(b.dayOfWeek),
  );

  const groupedHours = sortedHours.reduce<typeof sortedHours[]>((groups, current) => {
    const lastGroup = groups[groups.length - 1];

    const sameSchedule =
      lastGroup &&
      lastGroup[0].closed === current.closed &&
      lastGroup[0].openTime === current.openTime &&
      lastGroup[0].closeTime === current.closeTime;

    if (sameSchedule) {
      lastGroup.push(current);
    } else {
      groups.push([current]);
    }

    return groups;
  }, []);
  return (
    <section className="rounded-[1.75rem] border border-slate-200 bg-white p-6 shadow-sm">
      <div className="flex items-center gap-3">
        <span className="grid size-10 place-items-center rounded-xl bg-sky-100 text-sky-700">
          <Clock3 className="size-5" />
        </span>
        <h2 className="text-xl font-black text-slate-950">Giờ hoạt động</h2>
      </div>
      {!hours ? (
        <p className="mt-5 text-sm text-slate-500">
          Không thể tải giờ hoạt động.
        </p>
      ) : hours.length === 0 ? (
        <p className="mt-5 text-sm text-slate-500">
          Sân chưa cập nhật giờ hoạt động.
        </p>
      ) : (
        <dl className="mt-5 overflow-hidden rounded-2xl border border-slate-100">
          {groupedHours.map((group) => {
            const first = group[0];
            const last = group[group.length - 1];

            const dayLabel =
              group.length === 1
                ? formatDay(first.dayOfWeek)
                : `${formatDay(first.dayOfWeek)} - ${formatDay(last.dayOfWeek)}`;

            return (
              <div
                key={first.id ?? first.dayOfWeek}
                className="flex items-center justify-between gap-4 border-b border-slate-100 px-4 py-3.5 text-sm last:border-0"
              >
                <dt className="font-bold text-slate-800">{dayLabel}</dt>

                <dd
                  className={
                    first.closed
                      ? "font-semibold text-rose-600"
                      : "rounded-full bg-emerald-50 px-3 py-1 font-bold text-emerald-700"
                  }
                >
                  {first.closed
                    ? "Đóng cửa"
                    : `${formatTime(first.openTime)} – ${formatTime(first.closeTime)}`}
                </dd>
              </div>
            );
          })}
        </dl>
      )}
    </section>
  );
}
