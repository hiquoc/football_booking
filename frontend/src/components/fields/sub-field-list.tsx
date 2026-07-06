import { CalendarDays, Users } from "lucide-react";
import Link from "next/link";
import type { SubField } from "@/lib/api/types";
import { formatCurrency, formatEnum, formatTime } from "@/lib/field-format";

export function SubFieldList({ fields,isBookable }: { fields: SubField[] | null, isBookable?: boolean }) {
  const activeFields = fields?.filter((field) => field.active) ?? [];
  const fieldId = activeFields[0]?.fieldId; 

  return (
    <section id="sub-fields" className="scroll-mt-28">
      <p className="text-xs font-black uppercase tracking-[0.18em] text-sky-600">
        Lựa chọn sân
      </p>
      <div className="flex flex-col justify-between gap-4 sm:flex-row sm:items-center">
        <h2 className="mt-2 text-3xl font-black tracking-[-0.04em] text-slate-950">
          Các sân có thể đặt
        </h2>
        {fieldId ? (
          <Link
            href={`/fields/${fieldId}/book`}
            className="relative z-10 inline-flex w-fit items-center gap-2 rounded-full bg-sky-500 px-5 py-3 text-sm font-black text-white transition hover:bg-sky-400"
          >
            <CalendarDays className="size-4" /> Đặt sân ngay
          </Link>
        ) : null}
      </div>

      {!fields && isBookable ? (
        <Notice text="Không thể tải danh sách sân con." />
      ) : activeFields.length === 0 ? (
        <Notice text="Hiện chưa có sân con nào nhận đặt lịch." />
      ) : (
        <div className="mt-6 grid gap-5 md:grid-cols-2">
          {activeFields.map((field) => {
            return (
              <article
                key={field.id}
                className="rounded-[1.75rem] border border-slate-200 bg-white p-6 shadow-sm"
              >
                <div className="flex items-start justify-between gap-4">
                  <div>
                    <p className="text-xs font-black uppercase tracking-[0.13em] text-sky-600">
                      {formatEnum(field.subFieldType)}
                    </p>
                    <h3 className="mt-2 text-xl font-black text-slate-950">
                      {field.name}
                    </h3>
                  </div>
                  {field.maxPlayers ? (
                    <span className="inline-flex items-center gap-1.5 rounded-full bg-slate-100 px-3 py-1.5 text-xs font-bold text-slate-600">
                      <Users className="size-4" /> Tối đa {field.maxPlayers}
                    </span>
                  ) : null}
                </div>
                {field.description ? (
                  <p className="mt-3 text-sm leading-6 text-slate-500">
                    {field.description}
                  </p>
                ) : null}
                {field.timePriceRules?.length ? (
                  <div className="mt-5 rounded-2xl bg-slate-50 p-4">
                    {field.timePriceRules.map((rule) => (
                      <div
                        key={rule.id ?? `${rule.startTime}-${rule.endTime}`}
                        className="flex justify-between gap-4 py-1 text-sm"
                      >
                        <span className="text-slate-500">
                          {formatTime(rule.startTime)} –{" "}
                          {formatTime(rule.endTime)}
                        </span>
                        <strong className="text-slate-800">
                          {formatCurrency(Number(rule.hourlyPrice))}/giờ
                        </strong>
                      </div>
                    ))}
                  </div>
                ) : null}
              </article>
            );
          })}
        </div>
      )}
    </section>
  );
}

function Notice({ text }: { text: string }) {
  return (
    <p className="mt-6 rounded-2xl border border-dashed border-slate-300 bg-white p-8 text-center text-sm text-slate-500">
      {text}
    </p>
  );
}
