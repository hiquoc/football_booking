"use client";

import { AlertTriangle, X } from "lucide-react";
import { useState } from "react";
import { submitBanClient, submitUnbanClient } from "@/lib/client/moderation";

export function ClientBanButton({
  fieldId,
  userId,
  banned,
  showStatus = false,
  hideWhenUnbanned = false,
}: {
  fieldId: string;
  userId: string;
  banned: boolean;
  showStatus?: boolean;
  hideWhenUnbanned?: boolean;
}) {
  const [currentBanned, setCurrentBanned] = useState(banned);
  const [confirmOpen, setConfirmOpen] = useState(false);
  const [pending, setPending] = useState(false);
  const [hidden, setHidden] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function submit() {
    setPending(true);
    setError(null);
    try {
      const violation = currentBanned
        ? await submitUnbanClient(fieldId, userId)
        : await submitBanClient(fieldId, userId);
      setCurrentBanned(Boolean(violation.banned));
      setConfirmOpen(false);
      if (hideWhenUnbanned && !violation.banned) {
        setHidden(true);
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : "Không thể cập nhật trạng thái cấm");
    } finally {
      setPending(false);
    }
  }

  if (hidden) return null;

  return (
    <div className="flex flex-col gap-1">
      <div className="flex flex-wrap items-center justify-center gap-2">
        {showStatus ? <ClientBanStatus banned={currentBanned} /> : null}
        <button
          type="button"
          disabled={pending}
          onClick={() => setConfirmOpen(true)}
          className={`inline-flex justify-center rounded-lg px-3 py-2 text-xs font-black text-white disabled:cursor-not-allowed disabled:opacity-60 ${
            currentBanned ? "bg-green-600 hover:bg-green-700" : "bg-rose-600 hover:bg-rose-700"
          }`}
        >
          {pending ? "Đang xử lý..." : currentBanned ? "Gỡ cấm" : "Cấm"}
        </button>
      </div>
      {error && !confirmOpen ? <span className="max-w-48 text-xs font-semibold text-rose-600">{error}</span> : null}
      {confirmOpen ? (
        <ClientBanConfirmDialog
          banned={currentBanned}
          pending={pending}
          error={error}
          onClose={() => setConfirmOpen(false)}
          onConfirm={submit}
        />
      ) : null}
    </div>
  );
}

function ClientBanStatus({ banned }: { banned: boolean }) {
  return banned ? (
    <span className="inline-flex h-8 items-center rounded-lg bg-slate-500 px-3 text-xs font-black text-white">
      Đã cấm
    </span>
  ) : (
    <span className="inline-flex h-8 items-center rounded-lg bg-green-600 px-3 text-xs font-black text-white">
      Đang hoạt động
    </span>
  );
}

function ClientBanConfirmDialog({
  banned,
  pending,
  error,
  onClose,
  onConfirm,
}: {
  banned: boolean;
  pending: boolean;
  error: string | null;
  onClose: () => void;
  onConfirm: () => void;
}) {
  return (
    <div
      className="fixed inset-0 z-[90] grid place-items-center bg-slate-950/55 p-4"
      role="dialog"
      aria-modal="true"
      aria-labelledby="client-ban-confirm-title"
    >
      <form
        onSubmit={(event) => {
          event.preventDefault();
          onConfirm();
        }}
        className="w-full max-w-lg rounded-2xl border border-slate-200 bg-white p-5 shadow-2xl shadow-slate-950/20"
      >
        <div className="flex items-start justify-between gap-4">
          <div>
            <p className="inline-flex items-center gap-2 text-xs font-black uppercase text-amber-600">
              <AlertTriangle className="size-4" />
              Xác nhận thao tác
            </p>
            <h3 id="client-ban-confirm-title" className="mt-2 text-lg font-black text-slate-950">
              {banned ? "Gỡ cấm đặt sân" : "Cấm đặt sân"}
            </h3>
            <p className="mt-2 text-sm font-semibold text-slate-600">
              {banned
                ? "Người dùng này sẽ có thể đặt sân tại sân đã chọn sau khi được gỡ cấm."
                : "Người dùng này sẽ không thể đặt sân tại sân đã chọn sau khi bị cấm."}
            </p>
          </div>
          <button
            type="button"
            onClick={onClose}
            disabled={pending}
            className="rounded-full p-2 text-slate-500 hover:bg-slate-100 hover:text-slate-900 disabled:opacity-50"
            aria-label="Đóng"
          >
            <X className="size-5" />
          </button>
        </div>

        <p className="mt-4 rounded-xl border border-amber-200 bg-amber-50 px-4 py-3 text-sm font-semibold text-amber-800">
          {banned
            ? "Xác nhận chỉ khi bạn muốn cho phép người dùng này đặt sân trở lại."
            : "Xác nhận chỉ khi bạn muốn chặn người dùng này đặt sân tại sân đã chọn."}
        </p>

        {error ? (
          <p className="mt-3 rounded-lg border border-rose-200 bg-rose-50 px-3 py-2 text-sm font-semibold text-rose-700">
            {error}
          </p>
        ) : null}

        <div className="mt-5 flex flex-col-reverse gap-3 sm:flex-row sm:justify-end">
          <button
            type="button"
            onClick={onClose}
            disabled={pending}
            className="rounded-xl border border-slate-200 px-4 py-2.5 text-sm font-black text-slate-700 hover:bg-slate-50 disabled:opacity-50"
          >
            Hủy
          </button>
          <button
            type="submit"
            disabled={pending}
            className="rounded-xl bg-amber-500 px-4 py-2.5 text-sm font-black text-white hover:bg-amber-600 disabled:opacity-60"
          >
            {pending ? "Đang xử lý..." : banned ? "Xác nhận gỡ cấm" : "Xác nhận cấm"}
          </button>
        </div>
      </form>
    </div>
  );
}
