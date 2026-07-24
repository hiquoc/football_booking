"use client";

import { useState } from "react";
import { LoaderCircle, Plus, WalletCards, X } from "lucide-react";
import { formatCurrency } from "@/lib/field-format";
import { useCreateCheckout } from "@/lib/hooks/use-payments";
import { useCurrentUser } from "@/lib/hooks/use-profile";

const TOP_UP_AMOUNTS = [20000, 30000, 40000, 50000] as const;

export function WalletBalance() {
  const currentUser = useCurrentUser();
  const checkout = useCreateCheckout();
  const [open, setOpen] = useState(false);
  const [amount, setAmount] = useState<number>(TOP_UP_AMOUNTS[0]);
  const balance = currentUser.data?.balance ?? 0;

  async function topUp() {
    const result = await checkout.mutateAsync({ amount, currency: "VND", provider: "STRIPE" });
    window.location.assign(result.checkoutUrl);
  }

  return (
    <div className="relative hidden items-center gap-2 rounded-full border border-slate-200 bg-white px-3 py-1.5 text-xs font-bold text-slate-700 sm:flex">
      <WalletCards className="size-4 text-sky-600" />
      <span className="leading-tight">
        <span className="block text-[10px] text-slate-400">Ví</span>
        <span>{currentUser.isPending ? "--" : formatCurrency(balance)}</span>
      </span>
      <button
        type="button"
        onClick={() => setOpen((value) => !value)}
        className="grid size-8 place-items-center rounded-full bg-sky-500 text-white hover:bg-sky-600"
        title="Nạp tiền"
      >
        <Plus className="size-4" />
      </button>

      {open ? (
        <div className="absolute right-0 top-12 z-50 w-72 rounded-2xl border border-slate-200 bg-white p-4 text-sm shadow-xl">
          <div className="flex items-start justify-between gap-3">
            <div>
              <h2 className="font-black text-slate-950">Nạp tiền</h2>
              <p className="mt-1 text-xs font-medium leading-5 text-slate-500">
                Chọn số tiền muốn nạp vào ví qua Stripe.
              </p>
            </div>
            <button
              type="button"
              onClick={() => setOpen(false)}
              className="grid size-8 shrink-0 place-items-center rounded-full text-slate-400 hover:bg-slate-100 hover:text-slate-700"
              title="Đóng"
            >
              <X className="size-4" />
            </button>
          </div>

          <label className="mt-4 block">
            <span className="mb-2 block text-xs font-bold text-slate-500">Số tiền</span>
            <select
              value={amount}
              onChange={(event) => setAmount(Number(event.target.value))}
              disabled={checkout.isPending}
              className="input-field"
            >
              {TOP_UP_AMOUNTS.map((value) => (
                <option key={value} value={value}>
                  {formatCurrency(value)}
                </option>
              ))}
            </select>
          </label>

          {checkout.error ? (
            <p className="mt-3 rounded-xl bg-rose-50 p-3 text-xs font-semibold text-rose-700">
              Không thể tạo phiên nạp ví. Vui lòng thử lại.
            </p>
          ) : null}

          <button
            type="button"
            onClick={topUp}
            disabled={checkout.isPending}
            className="action-button mt-4 w-full bg-sky-500 px-4 text-white hover:bg-sky-600 disabled:opacity-60"
          >
            {checkout.isPending ? <LoaderCircle className="size-4 animate-spin" /> : <Plus className="size-4" />}
            Nạp {formatCurrency(amount)}
          </button>
        </div>
      ) : null}
    </div>
  );
}
