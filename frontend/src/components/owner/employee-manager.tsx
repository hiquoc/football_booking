"use client";

import { useMemo, useState } from "react";
import Link from "next/link";
import { Phone, UserPlus, X } from "lucide-react";
import { useAssignFieldEmployee, useFieldEmployees, useRemoveFieldEmployee } from "@/lib/hooks/use-owner-fields";
import { useEmployeeByPhone } from "@/lib/hooks/use-users";

export function EmployeeManager({ fieldId, isOwner }: { fieldId: string; isOwner: boolean }) {
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
    <div>
      <div className="mb-6">
        <h1 className="text-2xl font-black text-slate-900">Nhân viên quản lý sân</h1>
      </div>
      <section className="mb-8 rounded-lg border border-slate-200 bg-white p-5">
        <h2 className="mb-4 text-base font-black text-slate-900">Nhân viên đã phân công</h2>
        {assigned.isLoading ? <p className="text-sm text-slate-500">Đang tải...</p> : null}
        {assigned.error ? <p className="text-sm font-semibold text-rose-600">{assigned.error.message}</p> : null}
        {!assigned.isLoading && !assigned.data?.length ? <p className="text-sm text-slate-500">Chưa có nhân viên nào được phân công.</p> : null}
        <div className="space-y-3">
          {(assigned.data ?? []).map((employee) => (
            <div key={employee.assignmentId} className="flex items-center justify-between rounded-lg border border-slate-200 px-4 py-3">
              <div>
                <Link href={`/users/${employee.employeeId}/profile`} className="font-bold text-slate-900 hover:text-green-800">
                  {employee.fullName ?? employee.phoneNumber ?? employee.employeeId}
                </Link>
                <p className="text-sm text-slate-500">{employee.phoneNumber ?? "Nhân viên"}</p>
              </div>
              {isOwner ? (
                <button type="button" onClick={() => remove.mutate(employee.employeeId)} disabled={remove.isPending} className="inline-flex items-center gap-2 rounded-lg border border-rose-200 px-3 py-2 text-sm font-bold text-rose-600 hover:bg-rose-50 disabled:opacity-60">
                  <X className="size-4" /> Gỡ
                </button>
              ) : null}
            </div>
          ))}
        </div>
      </section>

      {isOwner ? (
        <section className="rounded-lg border border-slate-200 bg-white p-5">
          <h2 className="mb-4 text-base font-black text-slate-900">Phân công nhân viên</h2>
          <p className="mb-4 rounded-lg border border-amber-200 bg-amber-50 px-3 py-2 text-sm font-semibold text-amber-800">
            Lưu ý: quyền nhân viên có thể mất tối đa 5 phút để có hiệu lực. Nếu nhân viên vừa được phân công chưa truy cập được trang quản lý, hãy yêu cầu họ đăng xuất rồi đăng nhập lại.
          </p>
          <div className="flex flex-col gap-3 sm:flex-row">
            <label className="flex flex-1 items-center gap-2 rounded-lg border border-slate-200 px-3 py-2">
              <Phone className="size-4 text-slate-400" />
              <input
                value={phoneNumber}
                onChange={(event) => setPhoneNumber(event.target.value)}
                placeholder="Nhập số điện thoại nhân viên"
                className="w-full bg-transparent text-sm outline-none"
              />
            </label>
            <button
              type="button"
              onClick={() => setLookupPhoneNumber(phoneNumber.trim())}
              disabled={!phoneNumber.trim() || employeeQuery.isFetching}
              className="inline-flex items-center justify-center gap-2 rounded-lg bg-green-600 px-4 py-2 text-sm font-bold text-white disabled:opacity-60"
            >
              Kiểm tra hồ sơ
            </button>
          </div>
          {employeeQuery.isFetching ? <p className="mt-3 text-sm text-slate-500">Đang kiểm tra...</p> : null}
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
                className="inline-flex items-center gap-2 rounded-lg bg-green-600 px-3 py-2 text-sm font-bold text-white disabled:opacity-60"
              >
                <UserPlus className="size-4" /> {assignedIds.has(employeeQuery.data.id) ? "Đã phân công" : "Phân công"}
              </button>
            </div>
          ) : null}
          {employeeQuery.error ? <p className="mt-3 text-sm font-semibold text-rose-600">{employeeQuery.error.message}</p> : null}
          {assign.error ? <p className="mt-3 text-sm font-semibold text-rose-600">{assign.error.message}</p> : null}
        </section>
      ) : null}
    </div>
  );
}
