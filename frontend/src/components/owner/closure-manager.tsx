"use client";

import { useState } from "react";
import { Check, LoaderCircle, Pencil, Plus, Trash2 } from "lucide-react";
import type { FieldClosure } from "@/lib/api/types";
import { formatDate } from "@/lib/field-format";
import { useFieldBookingData } from "@/lib/hooks/use-fields";
import {
  useClosures,
  useCreateClosure,
  useDeleteClosure,
  useUpdateClosure,
} from "@/lib/hooks/use-owner-fields";
import { DataEmpty, DataError, ListSkeleton } from "@/components/ui/data-state";

export function ClosureManager({ fieldId }: { fieldId: string }) {
  const subFields = useFieldBookingData(fieldId).subFields;
  const [selected, setSelected] = useState("");
  const selectedId = selected || subFields.data?.[0]?.id || "";
  const closures = useClosures(fieldId, selectedId);
  const create = useCreateClosure(fieldId);
  const remove = useDeleteClosure(fieldId);
  const update = useUpdateClosure(fieldId);
  const [show, setShow] = useState(false);
  const [editing, setEditing] = useState<FieldClosure | null>(null);
  const [selectedSubFieldIds, setSelectedSubFieldIds] = useState<string[]>([]);
  const availableSubFields = Array.from(
    new Map((subFields.data ?? []).map((item) => [item.id, item])).values(),
  );
  if (subFields.isPending) return <ListSkeleton />;
  if (subFields.isError) return <DataError title="Không thể tải sân con" />;
  if (!availableSubFields.length)
    return (
      <DataEmpty
        title="Chưa có sân con"
        description="Cần tạo sân con trước khi thiết lập lịch đóng."
      />
    );
  async function submit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    try {
      const input = {
        subFieldIds: editing ? [editing.subFieldId] : selectedSubFieldIds,
        startDate: String(form.get("startDate")),
        endDate: String(form.get("endDate")),
        reason: String(form.get("reason")),
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
      <div className="mb-6 flex flex-col justify-between gap-3 sm:flex-row">
        <select
          value={selectedId}
          onChange={(event) => setSelected(event.target.value)}
          className="input-field max-w-sm"
        >
          {availableSubFields.map((item) => (
            <option key={item.id} value={item.id}>
              {item.name}
            </option>
          ))}
        </select>
        <button
          onClick={() => {
            setEditing(null);
            setSelectedSubFieldIds([selectedId]);
            setShow((value) => !value);
          }}
          className="inline-flex items-center justify-center gap-2 rounded-full bg-green-600 px-5 py-3 text-sm font-black text-white"
        >
          <Plus className="size-4" /> Thêm lịch đóng
        </button>
      </div>
      {show ? (
        <form
          key={editing?.id ?? "new"}
          onSubmit={submit}
          className="mb-6 grid gap-4 rounded-2xl border border-slate-200 bg-slate-50 p-5 sm:grid-cols-2"
        >
          <fieldset className="sm:col-span-2">
            <div className="mb-3 flex items-center justify-between gap-3">
              <legend className="text-sm font-bold">Sân con áp dụng</legend>
              {!editing ? (
                <button
                  type="button"
                  onClick={() => setSelectedSubFieldIds(
                    selectedSubFieldIds.length === availableSubFields.length
                      ? []
                      : availableSubFields.map((item) => item.id),
                  )}
                  className="text-xs font-black text-green-700"
                >
                  {selectedSubFieldIds.length === availableSubFields.length ? "Bỏ chọn tất cả" : "Chọn tất cả"}
                </button>
              ) : null}
            </div>
            <div className="grid gap-2 sm:grid-cols-2 lg:grid-cols-3">
              {availableSubFields.map((item) => {
                const checked = editing
                  ? item.id === editing.subFieldId
                  : selectedSubFieldIds.includes(item.id);
                return (
                  <button
                    key={item.id}
                    type="button"
                    disabled={Boolean(editing)}
                    aria-pressed={checked}
                    onClick={() => setSelectedSubFieldIds((ids) =>
                      ids.includes(item.id)
                        ? ids.filter((id) => id !== item.id)
                        : [...ids, item.id],
                    )}
                    className={`flex items-center gap-3 rounded-xl border px-4 py-3 text-left text-sm font-semibold transition ${checked ? "border-green-600 bg-green-600 text-white" : "border-slate-200 bg-white text-slate-700 hover:border-green-300"}`}
                  >
                    <span className={`grid size-5 place-items-center rounded-md border ${checked ? "border-white bg-white text-green-700" : "border-slate-300"}`}>
                      {checked ? <Check className="size-3.5" /> : null}
                    </span>
                    {item.name}
                  </button>
                );
              })}
            </div>
            {!editing && !selectedSubFieldIds.length ? <p className="mt-2 text-sm font-semibold text-rose-600">Chọn ít nhất một sân con.</p> : null}
          </fieldset>
          <label>
            <span className="mb-2 block text-sm font-bold">Từ ngày</span>
            <input
              required
              name="startDate"
              type="date"
              defaultValue={editing?.startDate}
              className="input-field"
            />
          </label>
          <label>
            <span className="mb-2 block text-sm font-bold">Đến ngày</span>
            <input
              required
              name="endDate"
              type="date"
              defaultValue={editing?.endDate}
              className="input-field"
            />
          </label>
          <label className="sm:col-span-2">
            <span className="mb-2 block text-sm font-bold">Lý do</span>
            <input
              required
              name="reason"
              defaultValue={editing?.reason}
              className="input-field"
              placeholder="Bảo trì mặt sân..."
            />
          </label>
          {create.error || update.error ? (
            <p className="text-sm text-rose-700 sm:col-span-2">
              {(create.error ?? update.error)?.message}
            </p>
          ) : null}
          <button
            disabled={create.isPending || update.isPending || (!editing && !selectedSubFieldIds.length)}
            className="inline-flex w-fit items-center gap-2 rounded-full bg-slate-950 px-5 py-2.5 text-sm font-black text-white"
          >
            {create.isPending || update.isPending ? (
              <LoaderCircle className="size-4 animate-spin" />
            ) : null}{" "}
            {editing ? "Cập nhật lịch đóng" : "Lưu lịch đóng"}
          </button>
        </form>
      ) : null}
      {closures.isPending ? (
        <ListSkeleton />
      ) : closures.isError ? (
        <DataError title="Không thể tải lịch đóng" />
      ) : !closures.data.length ? (
        <DataEmpty
          title="Không có lịch đóng"
          description="Sân đang hoạt động theo lịch thông thường."
        />
      ) : (
        <div className="space-y-3">
          {closures.data.map((item) => (
            <article
              key={item.id}
              className="flex items-center justify-between gap-4 rounded-[1.25rem] border border-slate-200 bg-white p-5"
            >
              <div>
                <strong className="text-slate-900">
                  {formatDate(item.startDate)} – {formatDate(item.endDate)}
                </strong>
                <p className="mt-1 text-sm text-slate-500">{item.reason}</p>
              </div>
              <div className="flex gap-3">
                <button
                  onClick={() => {
                    setEditing(item);
                    setSelectedSubFieldIds([item.subFieldId]);
                    setShow(true);
                  }}
                  className="grid size-9 place-items-center rounded-full border border-green-100 text-green-700 hover:border-green-200 hover:bg-green-50"
                  aria-label="Chỉnh sửa lịch đóng"
                >
                  <Pencil className="size-4" />
                </button>
                <button
                  onClick={() => {
                    if (window.confirm("Xóa lịch đóng này?"))
                      remove.mutate(item.id);
                  }}
                  className="grid size-9 place-items-center rounded-full border border-rose-100 text-rose-600 hover:border-rose-200 hover:bg-rose-50"
                  aria-label="Xóa lịch đóng"
                >
                  <Trash2 className="size-4" />
                </button>
              </div>
            </article>
          ))}
        </div>
      )}
    </div>
  );
}
