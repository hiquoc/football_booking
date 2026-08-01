"use client";

import { useState } from "react";
import { LoaderCircle, Pencil, Plus, Trash2, X } from "lucide-react";
import type { SubField } from "@/lib/api/types";
import { formatCurrency, formatEnum } from "@/lib/field-format";
import { useFieldBookingData } from "@/lib/hooks/use-fields";
import { useSubFieldTypes } from "@/lib/hooks/use-field-types";
import {
  useCreateSubField,
  useDeleteSubField,
  useUpdateSubField,
} from "@/lib/hooks/use-owner-fields";
import { DataEmpty, DataError, ListSkeleton } from "@/components/ui/data-state";
import {
  closingTimeOptions,
  toClosingTimeInputValue,
  toClosingTimePayload,
} from "@/lib/time-format";
type PriceRule = { startTime: string; endTime: string; hourlyPrice: number };
const defaultPriceRule: PriceRule = { startTime: "06:00", endTime: "23:00", hourlyPrice: 200000 };
const priceRuleEndOptions = closingTimeOptions();
export function SubFieldManager({ fieldId }: { fieldId: string }) {
  const data = useFieldBookingData(fieldId);
  const create = useCreateSubField(fieldId);
  const update = useUpdateSubField(fieldId);
  const remove = useDeleteSubField(fieldId);
  const subFieldTypes = useSubFieldTypes();
  const [show, setShow] = useState(false);
  const [editing, setEditing] = useState<SubField | null>(null);
  const [priceRules, setPriceRules] = useState<PriceRule[]>([defaultPriceRule]);
  const typeOptions = subFieldTypes.data?.length
    ? subFieldTypes.data
    : editing?.subFieldType
      ? [editing.subFieldType]
      : [];
  function openForm(subField: SubField | null) {
    setEditing(subField);
    setPriceRules(subField?.timePriceRules?.length ? subField.timePriceRules.map((rule) => ({
      startTime: rule.startTime.slice(0, 5),
      endTime: toClosingTimeInputValue(rule.endTime, "23:00"),
      hourlyPrice: Number(rule.hourlyPrice),
    })) : [defaultPriceRule]);
    setShow(true);
  }
  if (data.subFields.isPending) return <ListSkeleton />;
  if (data.subFields.isError)
    return <DataError title="Không thể tải sân con" />;
  async function submit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    try {
      const input = {
        name: String(form.get("name")),
        description: String(form.get("description") || ""),
        active: true,
        subFieldType: String(form.get("type")),
        bookingRule: {
          minimumBookingDurationMinutes: Number(form.get("min")),
          maximumBookingDurationMinutes: Number(form.get("max")),
          bookingIntervalMinutes: Number(form.get("interval")),
        },
        timePriceRules: priceRules.map((rule) => ({
          startTime: `${rule.startTime}:00`,
          endTime: toClosingTimePayload(rule.endTime, "23:00:00"),
          hourlyPrice: rule.hourlyPrice,
        })),
      };
      if (editing) await update.mutateAsync({ id: editing.id, input });
      else await create.mutateAsync(input);
      setShow(false);
      setEditing(null);
    } catch {
      /* Rendered below. */
    }
  }
  return (
    <div>
      <button
        onClick={() => {
          if (show && !editing) setShow(false);
          else openForm(null);
        }}
        className="mb-5 inline-flex items-center gap-2 rounded-full bg-green-600 px-5 py-3 text-sm font-black text-white"
      >
        <Plus className="size-4" /> Thêm sân con
      </button>
      {show ? (
        <form
          key={editing?.id ?? "new"}
          onSubmit={submit}
          className="mb-6 grid gap-4 rounded-2xl border border-slate-200 bg-slate-50 p-5 sm:grid-cols-2"
        >
          <Input label="Tên sân">
            <input
              name="name"
              required
              defaultValue={editing?.name}
              className="input-field"
            />
          </Input>
          <Input label="Loại sân">
            <select
              name="type"
              defaultValue={editing?.subFieldType}
              className="input-field"
              disabled={subFieldTypes.isPending || subFieldTypes.isError || !typeOptions.length}
            >
              {subFieldTypes.isPending ? <option value="">Đang tải loại sân...</option> : null}
              {subFieldTypes.isError ? <option value="">Không thể tải loại sân</option> : null}
              {typeOptions.map((type) => (
                <option key={type} value={type}>
                  {formatEnum(type)}
                </option>
              ))}
            </select>
          </Input>
          <Input label="Thời lượng tối thiểu">
            <input
              name="min"
              type="number"
              defaultValue={
                editing?.bookingRule?.minimumBookingDurationMinutes ?? 60
              }
              min={15}
              className="input-field"
            />
          </Input>
          <Input label="Thời lượng tối đa">
            <input
              name="max"
              type="number"
              defaultValue={
                editing?.bookingRule?.maximumBookingDurationMinutes ?? 180
              }
              min={30}
              className="input-field"
            />
          </Input>
          <Input label="Bước thời gian">
            <input
              name="interval"
              type="number"
              defaultValue={editing?.bookingRule?.bookingIntervalMinutes ?? 30}
              min={15}
              className="input-field"
            />
          </Input>
          <div className="sm:col-span-2 rounded-2xl border border-slate-200 bg-white p-4">
            <div className="flex items-center justify-between gap-3">
              <h3 className="font-black">Giá theo khung giờ</h3>
              <button type="button" onClick={() => setPriceRules((rules) => [...rules, { ...defaultPriceRule }])} className="inline-flex items-center gap-2 rounded-full border border-green-200 px-3 py-2 text-xs font-black text-green-700"><Plus className="size-4" /> Thêm khung</button>
            </div>
            <div className="mt-4 space-y-3">
              {priceRules.map((rule, index) => (
                <div key={index} className="grid items-end gap-3 rounded-xl bg-slate-50 p-3 sm:grid-cols-[1fr_1fr_1.4fr_auto]">
                  <Input label="Từ giờ"><input required type="time" value={rule.startTime} onChange={(event) => setPriceRules((rules) => rules.map((item, i) => i === index ? { ...item, startTime: event.target.value } : item))} className="input-field" /></Input>
                  <Input label="Đến giờ"><input required type="time" list="price-rule-end-time-options" value={rule.endTime} onChange={(event) => setPriceRules((rules) => rules.map((item, i) => i === index ? { ...item, endTime: event.target.value } : item))} className="input-field" /></Input>
                  <Input label="Giá mỗi giờ"><input required type="number" min={1000} step={1000} value={rule.hourlyPrice} onChange={(event) => setPriceRules((rules) => rules.map((item, i) => i === index ? { ...item, hourlyPrice: Number(event.target.value) } : item))} className="input-field" /></Input>
                  <button type="button" disabled={priceRules.length === 1} onClick={() => setPriceRules((rules) => rules.filter((_, i) => i !== index))} className="grid size-11 place-items-center rounded-xl border border-rose-100 text-rose-600 disabled:opacity-30" aria-label="Xóa khung giá"><X className="size-4" /></button>
                </div>
              ))}
            </div>
          </div>
          <div className="sm:col-span-2">
            <Input label="Mô tả">
              <textarea
                name="description"
                rows={3}
                defaultValue={editing?.description ?? ""}
                className="input-field resize-none"
              />
            </Input>
          </div>
          {create.error || update.error ? (
            <p className="text-sm text-rose-700 sm:col-span-2">
              {(create.error ?? update.error)?.message}
            </p>
          ) : null}
          <datalist id="price-rule-end-time-options">
            {priceRuleEndOptions.map((option) => (
              <option key={option.value} value={option.value} label={option.label} />
            ))}
          </datalist>
          <button
            disabled={create.isPending || update.isPending}
            className="inline-flex w-fit items-center gap-2 rounded-full bg-slate-950 px-5 py-2.5 text-sm font-black text-white"
          >
            {create.isPending || update.isPending ? (
              <LoaderCircle className="size-4 animate-spin" />
            ) : null}{" "}
            {editing ? "Cập nhật sân con" : "Lưu sân con"}
          </button>
        </form>
      ) : null}
      {!data.subFields.data.length ? (
        <DataEmpty
          title="Chưa có sân con"
          description="Thêm sân con để khách hàng có thể chọn và đặt lịch."
        />
      ) : (
        <div className="grid gap-4 md:grid-cols-2">
          {data.subFields.data.map((item) => (
            <article
              key={item.id}
            className="rounded-2xl border border-slate-200 bg-white p-5"
            >
              <div className="flex justify-between gap-3">
                <div>
                  <p className="text-xs font-black uppercase text-green-700">
                    {formatEnum(item.subFieldType)}
                  </p>
                  <h2 className="mt-1 text-xl font-black">{item.name}</h2>
                </div>
                <div className="flex gap-2">
                  <button
                    onClick={() => {
                      openForm(item);
                    }}
                    className="grid size-9 place-items-center rounded-full border border-slate-200 text-green-700"
                    aria-label="Chỉnh sửa sân con"
                  >
                    <Pencil className="size-4" />
                  </button>
                  <button
                    onClick={() => {
                      if (window.confirm("Xóa sân con này?"))
                        remove.mutate(item.id);
                    }}
                    className="grid size-9 place-items-center rounded-full border border-slate-200 text-rose-600"
                    aria-label="Xóa sân con"
                  >
                    <Trash2 className="size-4" />
                  </button>
                </div>
              </div>
              <p className="mt-3 text-sm text-slate-500">
                {item.description || "Chưa có mô tả"}
              </p>
              <div className="mt-4 space-y-1 text-sm font-bold">
                {item.timePriceRules?.length ? item.timePriceRules.map((rule) => (
                  <p key={rule.id}>
                    {rule.startTime.slice(0, 5)}–{toClosingTimeInputValue(rule.endTime) === "23:59" ? "12:00 CH" : rule.endTime.slice(0, 5)}: {formatCurrency(Number(rule.hourlyPrice))}/giờ
                  </p>
                )) : <p>Chưa có giá</p>}
              </div>
            </article>
          ))}
        </div>
      )}
    </div>
  );
}
function Input({
  label,
  children,
}: {
  label: string;
  children: React.ReactNode;
}) {
  return (
    <label>
      <span className="mb-2 block text-sm font-bold">{label}</span>
      {children}
    </label>
  );
}
