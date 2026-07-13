"use client";

import { useState } from "react";
import {
  CalendarDays,
  CheckCircle2,
  Clock3,
  Layers3,
  LoaderCircle,
  ShieldCheck,
} from "lucide-react";
import {
  buildAvailableSlots,
  calculateBookingPrice,
  hidePastSlots,
} from "@/lib/booking-slots";
import { formatCurrency, formatEnum } from "@/lib/field-format";
import { useAvailability, useCreateBooking } from "@/lib/hooks/use-bookings";
import { useCurrentTime } from "@/lib/hooks/use-current-time";
import { useFieldBookingData } from "@/lib/hooks/use-fields";
import { useProfile } from "@/lib/hooks/use-profile";
import { DataEmpty, DataError, FormSkeleton } from "@/components/ui/data-state";
import type { PaymentMethod } from "@/lib/api/types";

export function BookingForm({
  fieldId,
  initialDate,
}: {
  fieldId: string;
  initialDate: string;
}) {
  const { field, subFields } = useFieldBookingData(fieldId);
  const [subFieldType, setSubFieldType] = useState("");
  const [subFieldId, setSubFieldId] = useState("");
  const [date, setDate] = useState(initialDate);
  const [startTime, setStartTime] = useState("");
  const [duration, setDuration] = useState(90);
  const [note, setNote] = useState("");
  const [paymentMethod, setPaymentMethod] = useState<PaymentMethod>("STRIPE");
  const createMutation = useCreateBooking();
  const profile = useProfile();
  const now = useCurrentTime();

  const activeSubFields = subFields.data?.filter((item) => item.active) ?? [];
  const subFieldTypes = [
    ...new Set(activeSubFields.map((item) => item.subFieldType)),
  ];
  const selectedType = subFieldType || subFieldTypes[0] || "";
  const candidates = activeSubFields.filter(
    (item) => item.subFieldType === selectedType,
  );
  const selectedId = candidates.some((item) => item.id === subFieldId)
    ? subFieldId
    : candidates[0]?.id || "";
  const selectedSubField = candidates.find((item) => item.id === selectedId);
  const bookingRule = selectedSubField?.bookingRule;
  const minimum = bookingRule?.minimumBookingDurationMinutes ?? 60;
  const maximum = bookingRule?.maximumBookingDurationMinutes ?? 180;
  const interval = bookingRule?.bookingIntervalMinutes ?? 30;
  const durationOptions = Array.from(
    { length: Math.floor((maximum - minimum) / interval) + 1 },
    (_, index) => minimum + index * interval,
  );
  const effectiveDuration = durationOptions.includes(duration)
    ? duration
    : durationOptions[0];
  const availability = useAvailability(selectedId, date);
  const availableSlots = buildAvailableSlots(
    availability.data,
    effectiveDuration,
    interval,
  );
  const slots = now ? hidePastSlots(availableSlots, date, now) : [];
  const selectedStartTime = slots.includes(startTime) ? startTime : "";
  const estimatedTotal = calculateBookingPrice(
    selectedSubField,
    selectedStartTime,
    effectiveDuration,
  );
  const isCheckingAvailability =
    !now || availability.isPending || availability.isFetching;

  if (field.isPending || subFields.isPending) return <FormSkeleton />;
  if (field.isError || subFields.isError)
    return <DataError title="Không thể tải thông tin đặt sân" />;
  if (!activeSubFields.length)
    return (
      <DataEmpty
        title="Chưa thể đặt sân"
        description="Địa điểm này hiện chưa có sân con đang hoạt động."
      />
    );

  function resetTimeSelection() {
    setStartTime("");
    createMutation.reset();
  }

  async function submit(event: React.FormEvent) {
    event.preventDefault();
    if (!selectedSubField || !selectedStartTime) return;
    try {
      const booking = await createMutation.mutateAsync({
        subFieldId: selectedSubField.id,
        bookingDate: date,
        startTime: `${selectedStartTime}:00`,
        durationMinutes: effectiveDuration,
        note: note.trim() || undefined,
        paymentMethod,
      });
      window.location.assign(paymentMethod === "STRIPE" ? `/bookings/${booking.id}/payment` : `/bookings/${booking.id}`);
    } catch {
      // React Query exposes the booking conflict below.
    }
  }

  return (
    <form
      onSubmit={submit}
      className="grid gap-7 lg:grid-cols-[minmax(0,1fr)_22rem]"
    >
      <div className="space-y-7 rounded-[2rem] border border-slate-200 bg-white p-6 shadow-sm sm:p-8">
        <div>
          <p className="text-sm font-bold text-sky-600">
            {field.data.name}
          </p>
          <h1 className="mt-1 text-3xl font-black text-slate-950">
            Chọn lịch thi đấu
          </h1>
          <p className="mt-2 text-sm leading-6 text-slate-500">
            Chọn loại sân, sân con cụ thể, ngày và khung giờ bạn muốn đặt.
          </p>
        </div>

        <BookingStep number="1" title="Chọn loại sân">
          <div className="grid gap-3 sm:grid-cols-2">
            {subFieldTypes.map((type) => {
              const count = activeSubFields.filter(
                (item) => item.subFieldType === type,
              ).length;
              return (
                <button
                  key={type}
                  type="button"
                  onClick={() => {
                    setSubFieldType(type);
                    setSubFieldId("");
                    resetTimeSelection();
                  }}
                  className={`flex items-center justify-between rounded-2xl border p-4 text-left transition ${selectedType === type ? "border-sky-500 bg-sky-50 ring-2 ring-sky-500/10" : "border-slate-200 hover:border-sky-300"}`}
                >
                  <span>
                    <strong className="block text-sm text-slate-900">
                      {formatEnum(type)}
                    </strong>
                    <span className="mt-1 block text-xs text-slate-500">
                      {count} sân đang hoạt động
                    </span>
                  </span>
                  <Layers3 className="size-5 text-sky-600" />
                </button>
              );
            })}
          </div>
        </BookingStep>

        <BookingStep number="2" title="Chọn sân con">
          <div className="grid gap-3 sm:grid-cols-2">
            {candidates.map((subField) => (
              <button
                key={subField.id}
                type="button"
                onClick={() => {
                  setSubFieldId(subField.id);
                  resetTimeSelection();
                }}
                className={`rounded-2xl border p-4 text-left transition ${selectedId === subField.id ? "border-sky-500 bg-sky-50 ring-2 ring-sky-500/10" : "border-slate-200 hover:border-sky-300"}`}
              >
                <strong className="block text-sm text-slate-900">
                  {subField.name}
                </strong>
                <span className="mt-1 block text-xs text-slate-500">
                  {subField.maxPlayers
                    ? `Tối đa ${subField.maxPlayers} người`
                    : formatEnum(subField.surfaceType)}
                </span>
              </button>
            ))}
          </div>
        </BookingStep>

        {selectedSubField ? (
          <section>
            <div className="mb-4 flex items-center gap-3">
              <span className="grid size-7 place-items-center rounded-full bg-sky-100 text-xs font-black text-sky-700">3</span>
              <h2 className="font-black text-slate-900">Bảng giá {selectedSubField.name}</h2>
            </div>
            <div className="overflow-hidden rounded-2xl border border-slate-200">
              {selectedSubField.timePriceRules.map((rule) => (
                <div key={rule.id} className="flex items-center justify-between border-b border-slate-100 px-4 py-3 text-sm last:border-0">
                  <span className="font-semibold text-slate-600">
                    {rule.startTime.slice(0, 5)} - {rule.endTime.slice(0, 5)}
                  </span>
                  <strong className="text-slate-950">{formatCurrency(rule.hourlyPrice)}/giờ</strong>
                </div>
              ))}
            </div>
          </section>
        ) : null}

        <BookingStep number="4" title="Chọn ngày và thời lượng">
          <div className="grid gap-4 sm:grid-cols-2">
            <Field label="Ngày đặt sân">
              <input
                type="date"
                min={initialDate}
                value={date}
                onChange={(event) => {
                  setDate(event.target.value);
                  resetTimeSelection();
                }}
                className="input-field"
              />
            </Field>
            <Field label="Thời lượng">
              <select
                value={effectiveDuration}
                onChange={(event) => {
                  setDuration(Number(event.target.value));
                  resetTimeSelection();
                }}
                className="input-field"
              >
                {durationOptions.map((value) => (
                  <option key={value} value={value}>
                    {value} phút
                  </option>
                ))}
              </select>
            </Field>
          </div>
        </BookingStep>

        <BookingStep number="5" title="Chọn giờ bắt đầu">
          {isCheckingAvailability ? (
            <div className="grid h-24 animate-pulse grid-cols-2 gap-2 sm:grid-cols-4" aria-label="Đang kiểm tra lịch trống">
              {[0, 1, 2, 3].map((item) => <div key={item} className="rounded-xl bg-slate-100" />)}
            </div>
          ) : availability.isError ? (
            <p className="rounded-xl bg-amber-50 p-4 text-sm text-amber-700">
              Không thể kiểm tra lịch trống. Vui lòng thử lại.
            </p>
          ) : slots.length ? (
            <div className="grid grid-cols-2 gap-2 sm:grid-cols-4">
              {slots.map((slot) => (
                <button
                  key={slot}
                  type="button"
                  onClick={() => setStartTime(slot)}
                  className={`rounded-xl border px-3 py-2.5 text-sm font-bold transition ${selectedStartTime === slot ? "border-sky-500 bg-sky-500 text-white" : "border-slate-200 hover:border-sky-300"}`}
                >
                  <span className="block">{slot}</span>
                  <span className="mt-0.5 block text-[10px] font-semibold opacity-70">
                    Còn trống
                  </span>
                </button>
              ))}
            </div>
          ) : (
            <p className="rounded-xl bg-slate-50 p-4 text-sm text-slate-500">
              Không còn khung giờ phù hợp trong ngày này.
            </p>
          )}
          {selectedStartTime && selectedSubField ? (
            <div className="mt-4 flex gap-3 rounded-2xl bg-sky-50 p-4 text-sm text-sky-800">
              <CheckCircle2 className="mt-0.5 size-5 shrink-0" />
              <p>
                Đã kiểm tra: <strong>{selectedSubField.name}</strong> còn trống
                trong khung giờ đã chọn.
              </p>
            </div>
          ) : null}
        </BookingStep>

        <Field label="Ghi chú cho chủ sân (không bắt buộc)">
          <textarea
            value={note}
            onChange={(event) => setNote(event.target.value)}
            maxLength={500}
            rows={4}
            className="input-field resize-none"
            placeholder="Ví dụ: chuẩn bị bóng, áo bib..."
          />
        </Field>

        <BookingStep number="6" title="Phương thức thanh toán">
          <div className="grid gap-3 sm:grid-cols-2">
            <button
              type="button"
              onClick={() => setPaymentMethod("STRIPE")}
              className={`rounded-2xl border p-4 text-left transition ${paymentMethod === "STRIPE" ? "border-sky-500 bg-sky-50 ring-2 ring-sky-500/10" : "border-slate-200 hover:border-sky-300"}`}
            >
              <strong className="block text-sm text-slate-900">Stripe</strong>
              <span className="mt-1 block text-xs text-slate-500">
                Thanh toán bằng thẻ hoặc ví hỗ trợ Stripe.
              </span>
            </button>
            <button
              type="button"
              onClick={() => setPaymentMethod("ACCOUNT_BALANCE")}
              className={`rounded-2xl border p-4 text-left transition ${paymentMethod === "ACCOUNT_BALANCE" ? "border-sky-500 bg-sky-50 ring-2 ring-sky-500/10" : "border-slate-200 hover:border-sky-300"}`}
            >
              <strong className="block text-sm text-slate-900">Số dư tài khoản</strong>
              <span className="mt-1 block text-xs text-slate-500">
                Hiện có {formatCurrency(profile.data?.balance ?? 0)}.
              </span>
            </button>
          </div>
        </BookingStep>
      </div>

      <aside className="h-fit rounded-[2rem] border border-sky-100 bg-sky-50 p-6 text-slate-900 lg:sticky lg:top-24">
        <h2 className="text-xl font-black">Tóm tắt đặt sân</h2>
        <dl className="mt-6 space-y-4 text-sm">
          <Summary
            icon={<Layers3 />}
            label="Loại sân"
            value={formatEnum(selectedType)}
          />
          <Summary
            icon={<Layers3 />}
            label="Sân con"
            value={selectedSubField?.name ?? "Chưa chọn"}
          />
          <Summary
            icon={<CalendarDays />}
            label="Ngày"
            value={new Date(`${date}T00:00:00`).toLocaleDateString("vi-VN")}
          />
          <Summary
            icon={<Clock3 />}
            label="Khung giờ"
            value={
              selectedStartTime
                ? `${selectedStartTime} · ${effectiveDuration} phút`
                : "Chưa chọn"
            }
          />
        </dl>
        <div className="mt-6 border-t border-slate-200 pt-5">
          <span className="text-sm text-slate-500">Tạm tính</span>
          <strong className="mt-1 block text-2xl">
            {estimatedTotal === null
              ? "Chọn giờ để xem giá"
              : formatCurrency(estimatedTotal)}
          </strong>
        </div>
        <p className="mt-4 flex gap-2 text-xs leading-5 text-slate-500">
          <ShieldCheck className="mt-0.5 size-4 shrink-0 text-sky-600" />
          Dữ liệu lịch trống đã được tải trước. Hệ thống vẫn xác nhận lần cuối
          để tránh hai người đặt cùng một khung giờ.
        </p>
        <p className="mt-3 rounded-xl bg-white/70 p-3 text-xs leading-5 text-slate-500">
          Tổng thanh toán cuối cùng có thể bao gồm phí đặt sân được cấu hình bởi hệ thống.
        </p>
        {createMutation.error ? (
          <p className="mt-4 rounded-xl bg-rose-50 p-3 text-sm text-rose-700">
            {createMutation.error.message}
          </p>
        ) : null}
        <button
          disabled={
            !selectedSubField ||
            !selectedStartTime ||
            createMutation.isPending
          }
          className="action-button mt-6 w-full bg-sky-500 px-5 text-white hover:bg-sky-600"
        >
          {createMutation.isPending ? (
            <LoaderCircle className="size-4 animate-spin" />
          ) : null}
          Tiếp tục thanh toán
        </button>
      </aside>
    </form>
  );
}

function BookingStep({
  number,
  title,
  children,
}: {
  number: string;
  title: string;
  children: React.ReactNode;
}) {
  return (
    <section>
      <div className="mb-4 flex items-center gap-3">
        <span className="grid size-7 place-items-center rounded-full bg-sky-100 text-xs font-black text-sky-700">
          {number}
        </span>
        <h2 className="font-black text-slate-900">{title}</h2>
      </div>
      {children}
    </section>
  );
}

function Field({
  label,
  children,
}: {
  label: string;
  children: React.ReactNode;
}) {
  return (
    <label className="block">
      <span className="mb-2 block text-sm font-bold text-slate-700">
        {label}
      </span>
      {children}
    </label>
  );
}

function Summary({
  icon,
  label,
  value,
}: {
  icon: React.ReactNode;
  label: string;
  value: string;
}) {
  return (
    <div className="flex items-center gap-3">
      <span className="text-sky-400 [&_svg]:size-4">{icon}</span>
      <div>
        <dt className="text-slate-500">{label}</dt>
        <dd className="font-bold text-slate-900">{value}</dd>
      </div>
    </div>
  );
}
