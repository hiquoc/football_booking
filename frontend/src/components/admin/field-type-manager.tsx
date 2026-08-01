"use client";

import { useState } from "react";
import { LoaderCircle, Pencil, Plus, Trash2, X } from "lucide-react";
import type { FieldType, FieldTypeInput } from "@/lib/api/types";
import { formatFieldType } from "@/lib/field-format";
import {
  useCreateFieldType,
  useDeleteFieldType,
  useFieldTypes,
  useUpdateFieldType,
} from "@/lib/hooks/use-field-types";
import { DataEmpty, DataError, ListSkeleton } from "@/components/ui/data-state";

const sportOptions = [
  "FOOTBALL",
  "BADMINTON",
  "TENNIS",
  "PICKLEBALL",
  "BASKETBALL",
  "VOLLEYBALL",
];

export function FieldTypeManager() {
  const types = useFieldTypes();
  const create = useCreateFieldType();
  const update = useUpdateFieldType();
  const remove = useDeleteFieldType();
  const [editing, setEditing] = useState<FieldType | null>(null);
  const [showForm, setShowForm] = useState(false);

  if (types.isPending) return <ListSkeleton />;
  if (types.isError)
    return <DataError title="Không thể tải danh sách loại sân" />;

  async function submit(input: FieldTypeInput) {
    try {
      if (editing) await update.mutateAsync({ id: editing.id, input });
      else await create.mutateAsync(input);
      setEditing(null);
      setShowForm(false);
    } catch {
      /* Mutation state renders the error. */
    }
  }

  return (
    <div>
      <div className="mb-5 flex justify-end">
        <button
          onClick={() => {
            setEditing(null);
            setShowForm(true);
          }}
          className="inline-flex items-center gap-2 rounded-xl bg-green-600 px-5 py-3 text-sm font-black text-white hover:bg-green-700 shadow-none"
        >
          <Plus className="size-4" /> Thêm loại sân
        </button>
      </div>
      {showForm ? (
        <FieldTypeForm
          initial={editing}
          pending={create.isPending || update.isPending}
          error={create.error ?? update.error}
          onSubmit={submit}
          onClose={() => {
            setShowForm(false);
            setEditing(null);
          }}
        />
      ) : null}
      {!types.data.length ? (
        <DataEmpty
          title="Chưa có loại sân"
          description="Thêm loại sân đầu tiên để chủ sân có thể cấu hình địa điểm."
        />
      ) : (
        <div className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
          {types.data.map((type) => (
            <div
              key={type.id}
              className="flex flex-col justify-between gap-4 border-b border-slate-100 p-5 last:border-0 sm:flex-row sm:items-center"
            >
              <div>
                <div className="flex items-center gap-2">
                  <h2 className="font-black text-slate-950">
                    {formatFieldType(type.name)}
                  </h2>
                  <span
                    className={`rounded-lg px-2 py-1 text-[10px] font-black ${type.active ? "bg-green-100 text-green-700" : "bg-slate-100 text-slate-500"}`}
                  >
                    {type.active ? "Đang hoạt động" : "Tạm ẩn"}
                  </span>
                </div>
                <p className="mt-1 text-sm text-slate-500">
                  Thời lượng mặc định: {type.defaultBookingDurationMinutes} phút
                </p>
              </div>
              <div className="flex gap-2">
                <button
                  onClick={() => {
                    setEditing(type);
                    setShowForm(true);
                  }}
                  className="grid size-9 place-items-center rounded-xl border border-slate-200 bg-white text-green-700 hover:border-green-300 hover:bg-green-50 shadow-none"
                  aria-label="Chỉnh sửa"
                >
                  <Pencil className="size-4" />
                </button>
                <button
                  onClick={() => {
                    if (window.confirm("Xóa loại sân này?"))
                      remove.mutate(type.id);
                  }}
                  disabled={remove.isPending}
                  className="grid size-9 place-items-center rounded-xl bg-rose-500 text-white hover:bg-rose-600 disabled:cursor-wait disabled:opacity-60 shadow-none"
                  aria-label="Xóa"
                >
                  <Trash2 className="size-4" />
                </button>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

function FieldTypeForm({
  initial,
  pending,
  error,
  onSubmit,
  onClose,
}: {
  initial: FieldType | null;
  pending: boolean;
  error: Error | null;
  onSubmit: (input: FieldTypeInput) => Promise<void>;
  onClose: () => void;
}) {
  const [name, setName] = useState(initial?.name ?? "FOOTBALL");
  const [duration, setDuration] = useState(
    initial?.defaultBookingDurationMinutes ?? 60,
  );
  const [active, setActive] = useState(initial?.active ?? true);
  const [description, setDescription] = useState("");
  return (
    <form
      onSubmit={(event) => {
        event.preventDefault();
        void onSubmit({
          name,
          defaultBookingDurationMinutes: duration,
          description,
          active,
        });
      }}
      className="mb-6 rounded-2xl border border-slate-200 bg-white p-5 shadow-sm"
    >
      <div className="flex items-center justify-between">
        <h2 className="font-black text-slate-950">
          {initial ? "Chỉnh sửa loại sân" : "Thêm loại sân"}
        </h2>
        <button
          type="button"
          onClick={onClose}
          className="grid size-9 place-items-center rounded-xl border border-slate-200 bg-white text-slate-700 hover:border-green-300 hover:text-green-700 shadow-none"
          aria-label="Đóng biểu mẫu"
        >
          <X className="size-5" />
        </button>
      </div>
      <div className="mt-5 grid gap-4 sm:grid-cols-2">
        <label>
          <span className="mb-2 block text-sm font-bold">Môn thể thao</span>
          <select
            className="input-field"
            value={name}
            onChange={(event) => setName(event.target.value)}
          >
            {sportOptions.map((option) => (
              <option key={option} value={option}>
                {formatFieldType(option)}
              </option>
            ))}
          </select>
        </label>
        <label>
          <span className="mb-2 block text-sm font-bold">
            Thời lượng mặc định
          </span>
          <input
            className="input-field"
            type="number"
            min={15}
            step={15}
            value={duration}
            onChange={(event) => setDuration(Number(event.target.value))}
          />
        </label>
        <label className="sm:col-span-2">
          <span className="mb-2 block text-sm font-bold">Mô tả</span>
          <textarea
            className="input-field resize-none"
            rows={3}
            value={description}
            onChange={(event) => setDescription(event.target.value)}
          />
        </label>
        <label className="inline-flex items-center gap-2 text-sm font-bold">
          <input
            type="checkbox"
            checked={active}
            onChange={(event) => setActive(event.target.checked)}
          />{" "}
          Đang hoạt động
        </label>
      </div>
      {error ? (
        <p className="mt-4 text-sm text-rose-700">{error.message}</p>
      ) : null}
      <button
        disabled={pending}
        className="mt-5 inline-flex items-center gap-2 rounded-xl bg-green-600 px-5 py-2.5 text-sm font-black text-white hover:bg-green-700 disabled:cursor-wait disabled:opacity-60 shadow-none"
      >
        {pending ? <LoaderCircle className="size-4 animate-spin" /> : null}
        Lưu loại sân
      </button>
    </form>
  );
}
