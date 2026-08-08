"use client";

import { Check, ChevronDown, Loader2, Search, X } from "lucide-react";
import { useEffect, useMemo, useRef, useState } from "react";
import type { FieldSearchOption } from "@/lib/api/types";
import { useAdminFieldSearch } from "@/lib/hooks/use-fields";

type SearchableFieldSelectProps = {
  value?: string[];
  selectedFields?: FieldSearchOption[];
  onChange: (fieldIds: string[], selectedFields: FieldSearchOption[]) => void;
  placeholder?: string;
  disabled?: boolean;
};

export function SearchableFieldSelect({
  value = [],
  selectedFields = [],
  onChange,
  placeholder = "Tìm sân...",
  disabled = false,
}: SearchableFieldSelectProps) {
  const containerRef = useRef<HTMLDivElement>(null);
  const [open, setOpen] = useState(false);
  const [keyword, setKeyword] = useState("");
  const [debouncedKeyword, setDebouncedKeyword] = useState("");
  const selectedIds = useMemo(() => Array.from(new Set(value.filter(Boolean))), [value]);
  const selected = useMemo(() => {
    const byId = new Map(selectedFields.map((field) => [field.fieldId, field]));
    return selectedIds.map((fieldId) => byId.get(fieldId) ?? { fieldId, name: fieldId });
  }, [selectedFields, selectedIds]);
  const searchQuery = useAdminFieldSearch(debouncedKeyword, open && !disabled);

  useEffect(() => {
    const timeout = window.setTimeout(() => setDebouncedKeyword(keyword.trim()), 500);
    return () => window.clearTimeout(timeout);
  }, [keyword]);

  useEffect(() => {
    function closeOnOutsideClick(event: MouseEvent) {
      if (!containerRef.current?.contains(event.target as Node)) {
        setOpen(false);
      }
    }
    document.addEventListener("mousedown", closeOnOutsideClick);
    return () => document.removeEventListener("mousedown", closeOnOutsideClick);
  }, []);

  function selectField(field: FieldSearchOption) {
    if (selectedIds.includes(field.fieldId)){
      removeField(field.fieldId);
      return;
    }
    onChange([...selectedIds, field.fieldId], [...selected, field]);
    setKeyword("");
  }

  function removeField(fieldId: string) {
    const nextSelected = selected.filter((field) => field.fieldId !== fieldId);
    onChange(nextSelected.map((field) => field.fieldId), nextSelected);
  }

  function selectAllFields() {
    onChange([], []);
    setKeyword("");
    setOpen(false);
  }

  const results = searchQuery.data ?? [];

  return (
    <div ref={containerRef} className="relative">
      <button
        type="button"
        disabled={disabled}
        onClick={() => setOpen((current) => !current)}
        className="flex min-h-11 w-full items-center justify-between gap-3 rounded-xl border border-slate-200 bg-slate-50 px-3 py-2 text-left text-sm font-semibold text-slate-700 outline-none transition focus:border-green-500 focus:bg-white focus:ring-4 focus:ring-green-100 disabled:cursor-not-allowed disabled:opacity-60"
      >
        <span className="flex min-w-0 flex-1 flex-wrap gap-2">
          {selected.length ? (
            selected.map((field) => (
              <span key={field.fieldId} className="inline-flex max-w-full items-center gap-1 rounded-lg bg-green-50 px-2.5 py-1 text-xs font-black text-green-700">
                <span className="truncate">{field.name}</span>
                <span
                  role="button"
                  tabIndex={-1}
                  onClick={(event) => {
                    event.stopPropagation();
                    removeField(field.fieldId);
                  }}
                  className="rounded-full p-0.5 hover:bg-green-100"
                  aria-label={`Bỏ chọn ${field.name}`}
                >
                  <X className="size-3.5" />
                </span>
              </span>
            ))
          ) : (
            <span className="inline-flex rounded-lg bg-slate-100 px-2.5 py-1 text-xs font-black text-slate-600">Tất cả sân</span>
          )}
        </span>
        <ChevronDown className={`size-4 shrink-0 text-slate-400 transition ${open ? "rotate-180" : ""}`} />
      </button>

      {open ? (
        <div className="absolute left-0 right-0 z-50 mt-2 overflow-hidden rounded-xl border border-slate-200 bg-white shadow-xl shadow-slate-950/10">
          <div className="border-b border-slate-100 p-3">
            <div className="flex items-center gap-2 rounded-lg border border-slate-200 bg-slate-50 px-3 py-2 focus-within:border-green-500 focus-within:bg-white focus-within:ring-4 focus-within:ring-green-100">
              <Search className="size-4 text-slate-400" />
              <input
                value={keyword}
                onChange={(event) => setKeyword(event.target.value)}
                placeholder={placeholder}
                className="min-w-0 flex-1 bg-transparent text-sm font-semibold text-slate-700 outline-none placeholder:text-slate-400"
                autoFocus
              />
            </div>
          </div>

          <div className="max-h-72 overflow-y-auto p-2">
            <button
              type="button"
              onClick={selectAllFields}
              className="flex w-full items-center gap-3 rounded-lg px-3 py-2 text-left text-sm font-bold text-slate-700 hover:bg-slate-50"
            >
              <span className="grid size-5 place-items-center rounded border border-slate-300 bg-white">
                {!selected.length ? <Check className="size-3.5 text-green-600" /> : null}
              </span>
              Tất cả sân
            </button>

            {searchQuery?.isFetching ? (
              <div className="flex items-center gap-2 px-3 py-4 text-sm font-semibold text-slate-500">
                <Loader2 className="size-4 animate-spin" />
                Đang tìm sân...
              </div>
            ) : null}

            {searchQuery.isError ? (
              <p className="px-3 py-4 text-sm font-semibold text-rose-600">Không thể tải danh sách sân.</p>
            ) : null}

            {!searchQuery.isFetching && !searchQuery.isError && !results.length ? (
              <p className="px-3 py-4 text-sm font-semibold text-slate-500">Không tìm thấy sân phù hợp.</p>
            ) : null}

            {!searchQuery.isError
              ? results.map((field) => {
                  const checked = selectedIds.includes(field.fieldId);
                  return (
                    <button
                      key={field.fieldId}
                      type="button"
                      // disabled={checked}
                      onClick={() => selectField(field)}
                      className="flex w-full items-center gap-3 rounded-lg px-3 py-2 text-left text-sm font-bold text-slate-700 hover:bg-slate-50 disabled:cursor-default disabled:bg-green-50 disabled:text-green-700"
                    >
                      <span className="grid size-5 place-items-center rounded border border-slate-300 bg-white">
                        {checked ? <Check className="size-3.5 text-green-600" /> : null}
                      </span>
                      <span className="min-w-0 flex-1 truncate">{field.name}</span>
                    </button>
                  );
                })
              : null}
          </div>
        </div>
      ) : null}
    </div>
  );
}
