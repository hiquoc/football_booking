"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState } from "react";
import type { ReactNode } from "react";
import { ClientBanButton } from "@/components/owner/client-ban-button";
import { DataError, ListSkeleton } from "@/components/ui/data-state";
import type { Field } from "@/lib/api/types";
import { useFieldViolations } from "@/lib/hooks/use-moderation";

type FieldOption = Pick<Field, "id" | "name">;

export function ClientViolationsPanel({
  fields,
  selectedFieldId,
  page,
  requestedDenied,
}: {
  fields: FieldOption[];
  selectedFieldId: string;
  page: number;
  requestedDenied: boolean;
}) {
  const router = useRouter();
  const [currentFieldId, setCurrentFieldId] = useState(selectedFieldId);
  const [currentPage, setCurrentPage] = useState(page);
  const query = useFieldViolations(currentFieldId, currentPage, 20);

  function selectField(fieldId: string) {
    setCurrentFieldId(fieldId);
    setCurrentPage(0);
    const params = new URLSearchParams();
    if (fieldId) params.set("fieldId", fieldId);
    router.replace(`/owner/client-violations${params.size ? `?${params}` : ""}`, { scroll: false });
  }

  return (
    <>
      <FieldFilter fields={fields} selectedFieldId={currentFieldId} onChange={selectField} />
      {requestedDenied ? (
        <p className="mt-3 text-sm font-semibold text-amber-700">
          San duoc yeu cau khong thuoc tai khoan cua ban, nen danh sach da duoc chuyen ve san hop le.
        </p>
      ) : null}
      <div className="mt-6 overflow-hidden rounded-2xl border border-slate-200 bg-white">
        {query.isPending ? <ListSkeleton /> : null}
        {query.isError ? <DataError title="Khong the tai luot vang mat" /> : null}
        {query.data ? (
          <table className="w-full text-center text-sm">
            <thead className="bg-slate-50 text-slate-500">
              <tr>
                <th className="px-4 py-3">Khach</th>
                <th className="px-4 py-3">So lan vi pham</th>
                <th className="px-4 py-3">Vi pham gan nhat</th>
                <th className="px-4 py-3">Trang thai / thao tac</th>
              </tr>
            </thead>
            <tbody>
              {query.data.content.map((item) => (
                <tr key={item.id} className="border-t border-slate-100">
                  <td className="px-4 py-3 font-medium text-slate-900">
                    <Link href={`/users/${item.userId}/profile`} className="font-black text-green-700 hover:text-green-800">
                      {item.username ?? item.userDisplayName ?? item.phoneNumber ?? item.userId}
                    </Link>
                    {item.phoneNumber ? <p className="mt-1 text-xs font-semibold text-slate-500">{item.phoneNumber}</p> : null}
                  </td>
                  <td className="px-4 py-3">{item.violationCount}</td>
                  <td className="px-4 py-3">{item.lastViolationDate ? new Date(item.lastViolationDate).toLocaleDateString() : "-"}</td>
                  <td className="px-4 py-3">
                    <ClientBanButton fieldId={currentFieldId} userId={item.userId} banned={item.banned} showStatus />
                  </td>
                </tr>
              ))}
              {!query.data.content.length ? (
                <tr className="border-t border-slate-100">
                  <td className="px-4 py-8 text-center text-slate-500" colSpan={4}>
                    {currentFieldId ? "Chua co vi pham nao cho san nay." : "Tai khoan cua ban chua co san de quan ly."}
                  </td>
                </tr>
              ) : null}
            </tbody>
          </table>
        ) : null}
      </div>
      {query.data && query.data.totalPages > 1 ? (
        <div className="mt-6 flex items-center justify-center gap-3">
          {currentPage > 0 ? <PageLink fieldId={currentFieldId} page={currentPage - 1}>Truoc</PageLink> : null}
          <span className="text-sm font-semibold text-slate-500">Trang {query.data.page + 1}/{Math.max(query.data.totalPages, 1)}</span>
          {!query.data.last ? <PageLink fieldId={currentFieldId} page={currentPage + 1} primary>Sau</PageLink> : null}
        </div>
      ) : null}
    </>
  );
}

function FieldFilter({ fields, selectedFieldId, onChange }: { fields: FieldOption[]; selectedFieldId: string; onChange: (fieldId: string) => void }) {
  return (
    <div className="mt-6 rounded-2xl border border-slate-200 bg-white p-4">
      <label className="block text-sm font-semibold text-slate-700">
        San
        <select
          value={selectedFieldId}
          onChange={(event) => onChange(event.target.value)}
          className="mt-2 w-full rounded-xl border border-slate-200 bg-white px-3 py-2 text-sm text-slate-900 outline-none focus:border-green-500 focus:ring-4 focus:ring-green-100"
          disabled={!fields.length}
        >
          {!fields.length ? <option value="">Chua co san quan ly</option> : null}
          {fields.map((field) => <option key={field.id} value={field.id}>{field.name}</option>)}
        </select>
      </label>
    </div>
  );
}

function PageLink({ fieldId, page, primary = false, children }: { fieldId: string; page: number; primary?: boolean; children: ReactNode }) {
  const params = new URLSearchParams({ fieldId, page: String(page) });
  return (
    <Link href={`/owner/client-violations?${params}`} className={primary ? "rounded-xl bg-green-600 px-4 py-2 text-sm font-bold text-white" : "rounded-xl border border-slate-200 bg-white px-4 py-2 text-sm font-bold text-slate-700"}>
      {children}
    </Link>
  );
}
