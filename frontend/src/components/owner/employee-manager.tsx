"use client";

import { useMemo, useState } from "react";
import { Phone, UserPlus, X } from "lucide-react";
import { useAssignFieldEmployee, useFieldEmployees, useRemoveFieldEmployee } from "@/lib/hooks/use-owner-fields";
import { useEmployeeByPhone } from "@/lib/hooks/use-users";

export function EmployeeManager({ fieldId }: { fieldId: string }) {
  const [phoneNumber, setPhoneNumber] = useState("");
  const [lookupPhoneNumber, setLookupPhoneNumber] = useState("");
  const assigned = useFieldEmployees(fieldId);
  const employeeQuery = useEmployeeByPhone(lookupPhoneNumber, Boolean(lookupPhoneNumber));
  const assign = useAssignFieldEmployee(fieldId);
  const remove = useRemoveFieldEmployee(fieldId);
  const assignedIds = useMemo(
    () => new Set((assigned.data ?? []).map((employee) => employee.employeeId)),
    [assigned.data],
  );

  return (
    <div className="mx-auto max-w-3xl">
      <div className="mb-6">
        <h1 className="text-2xl font-black text-slate-900">Nhan vien quan ly san</h1>
      </div>
      <section className="mb-8 rounded-lg border border-slate-200 bg-white p-5">
        <h2 className="mb-4 text-base font-black text-slate-900">Nhan vien da phan cong</h2>
        {assigned.isLoading ? <p className="text-sm text-slate-500">Dang tai...</p> : null}
        {assigned.error ? <p className="text-sm font-semibold text-rose-600">{assigned.error.message}</p> : null}
        {!assigned.isLoading && !assigned.data?.length ? <p className="text-sm text-slate-500">Chua co nhan vien nao duoc phan cong.</p> : null}
        <div className="space-y-3">
          {(assigned.data ?? []).map((employee) => (
            <div key={employee.assignmentId} className="flex items-center justify-between rounded-lg border border-slate-200 px-4 py-3">
              <div>
                <p className="font-bold text-slate-900">{employee.fullName ?? employee.phoneNumber ?? employee.employeeId}</p>
                <p className="text-sm text-slate-500">{employee.phoneNumber ?? "Employee"}</p>
              </div>
              <button type="button" onClick={() => remove.mutate(employee.employeeId)} disabled={remove.isPending} className="inline-flex items-center gap-2 rounded-lg border border-rose-200 px-3 py-2 text-sm font-bold text-rose-600 hover:bg-rose-50 disabled:opacity-60">
                <X className="size-4" /> Go
              </button>
            </div>
          ))}
        </div>
      </section>
      <section className="rounded-lg border border-slate-200 bg-white p-5">
        <h2 className="mb-4 text-base font-black text-slate-900">Phan cong nhan vien</h2>
        <div className="flex flex-col gap-3 sm:flex-row">
          <label className="flex flex-1 items-center gap-2 rounded-lg border border-slate-200 px-3 py-2">
            <Phone className="size-4 text-slate-400" />
            <input
              value={phoneNumber}
              onChange={(event) => setPhoneNumber(event.target.value)}
              placeholder="Nhap so dien thoai nhan vien"
              className="w-full bg-transparent text-sm outline-none"
            />
          </label>
          <button
            type="button"
            onClick={() => setLookupPhoneNumber(phoneNumber.trim())}
            disabled={!phoneNumber.trim() || employeeQuery.isFetching}
            className="inline-flex items-center justify-center gap-2 rounded-lg bg-slate-950 px-4 py-2 text-sm font-bold text-white hover:bg-sky-600 disabled:opacity-60"
          >
            Kiem tra ho so
          </button>
        </div>
        {employeeQuery.isFetching ? <p className="mt-3 text-sm text-slate-500">Dang kiem tra...</p> : null}
        {employeeQuery.data ? (
          <div className="mt-4 flex items-center justify-between rounded-lg border border-slate-200 px-4 py-3">
            <div>
              <p className="font-bold text-slate-900">{employeeQuery.data.fullName ?? employeeQuery.data.phoneNumber}</p>
              <p className="text-sm text-slate-500">{employeeQuery.data.phoneNumber}</p>
              {employeeQuery.data.email ? <p className="text-sm text-slate-500">{employeeQuery.data.email}</p> : null}
            </div>
            <button
              type="button"
              onClick={() => assign.mutate(employeeQuery.data.id)}
              disabled={assign.isPending || assignedIds.has(employeeQuery.data.id)}
              className="inline-flex items-center gap-2 rounded-lg bg-sky-500 px-3 py-2 text-sm font-bold text-white hover:bg-sky-600 disabled:opacity-60"
            >
              <UserPlus className="size-4" /> {assignedIds.has(employeeQuery.data.id) ? "Da phan cong" : "Phan cong"}
            </button>
          </div>
        ) : null}
        {employeeQuery.error ? <p className="mt-3 text-sm font-semibold text-rose-600">{employeeQuery.error.message}</p> : null}
        {assign.error ? <p className="mt-3 text-sm font-semibold text-rose-600">{assign.error.message}</p> : null}
      </section>
    </div>
  );
}
