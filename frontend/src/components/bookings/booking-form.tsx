"use client";

import { useState } from "react";
import {
  CalendarDays,
  CheckCircle2,
  Clock3,
  Layers3,
  LoaderCircle,
} from "lucide-react";
import {
  type AvailableSlotOption,
  buildAvailableSlotOptions,
  calculateBookingPrice,
  hidePastSlotOptions,
} from "@/lib/booking-slots";
import { formatCurrency, formatEnum } from "@/lib/field-format";
import {
  useAvailability,
  useBookingConfig,
  useCreateBooking,
  useCreateReservation,
} from "@/lib/hooks/use-bookings";
import { useCreateRecurringBooking } from "@/lib/hooks/use-recurring-bookings";
import { useCurrentTime } from "@/lib/hooks/use-current-time";
import { useFieldBookingData } from "@/lib/hooks/use-fields";
import { useCurrentUser } from "@/lib/hooks/use-profile";
import { DataEmpty, DataError, FormSkeleton } from "@/components/ui/data-state";

const DEFAULT_FIRST_BOOKING_FEE = 5000;
const DEFAULT_RETURNING_BOOKING_FEE = 1000;

export function BookingForm({
  fieldId,
  initialDate,
  reservationMode = false,
}: {
  fieldId: string;
  initialDate: string;
  reservationMode?: boolean;
}) {
  const { field, subFields } = useFieldBookingData(fieldId);
  const [subFieldType, setSubFieldType] = useState("");
  const [subFieldId, setSubFieldId] = useState("");
  const [date, setDate] = useState(initialDate);
  const [selectedSlotKey, setSelectedSlotKey] = useState("");
  const [duration, setDuration] = useState(90);
  const [note, setNote] = useState("");
  const [recurringEnabled, setRecurringEnabled] = useState(false);
  const [recurringIntervalDays, setRecurringIntervalDays] = useState(7);
  const [isProcessing, setIsProcessing] = useState(false);
  const addDays = (dateString: string, days: number) => {
    if (!dateString || !days) return dateString;
    const d = new Date(dateString);
    d.setDate(d.getDate() + days);
    return d.toISOString().split("T")[0]; // yyyy-MM-dd
  };

  const [recurringEndDate, setRecurringEndDate] = useState(addDays(date, 7));
  const createMutation = useCreateBooking();
  const createReservationMutation = useCreateReservation();
  const createRecurringMutation = useCreateRecurringBooking();
  const currentUser = useCurrentUser();
  const bookingConfig = useBookingConfig();
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
  const availableSlots = buildAvailableSlotOptions(
    availability.data,
    effectiveDuration,
    interval,
    date,
  );
  const slots = now ? hidePastSlotOptions(availableSlots, date, now) : [];
  const selectedSlot: AvailableSlotOption | undefined = slots.find(
    (slot) => slot.key === selectedSlotKey,
  );
  const selectedStartTime = selectedSlot?.time ?? "";
  const estimatedTotal = calculateBookingPrice(
    selectedSubField,
    selectedStartTime,
    effectiveDuration,
  );
  const completedBookingCount = currentUser.data?.completedBookingCount;
  const platformBookingFee =
    completedBookingCount === undefined || completedBookingCount === 0
      ? (bookingConfig.data?.firstBookingFee ?? DEFAULT_FIRST_BOOKING_FEE)
      : (bookingConfig.data?.notFirstBookingFee ?? DEFAULT_RETURNING_BOOKING_FEE);
  const estimatedTotalWithFee = reservationMode ? 0 : platformBookingFee;
  const walletBalance = currentUser.data ? currentUser.data.balance : null;
  const remainingBalance =
    estimatedTotalWithFee === null || walletBalance === null
      ? null
      : walletBalance - estimatedTotalWithFee;
  const hasEnoughBalance = remainingBalance !== null && remainingBalance >= 0;
  const isFeeReady = estimatedTotalWithFee !== null && walletBalance !== null;
  const isCheckingAvailability =
    !now || availability.isPending || availability.isFetching;
  const recurringPreview = buildRecurringPreview(
    selectedSlot?.date ?? date,
    recurringEndDate,
    recurringIntervalDays,
  );


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
    setSelectedSlotKey("");
    createMutation.reset();
    createRecurringMutation.reset();
  }
  async function submit(event: React.FormEvent) {
    event.preventDefault();
    setIsProcessing(true);
    if (createMutation.isPending || createRecurringMutation.isPending || createReservationMutation.isPending) return;
    if (!selectedSubField || !selectedSlot || (!reservationMode && !isFeeReady)) return;
    if (!reservationMode && !hasEnoughBalance) {
      setIsProcessing(false);
      return;
    }
    try {
      const start = new Date(selectedSlot.startDateTime);
      const end = new Date(start.getTime() + effectiveDuration * 60_000);
      const slotDate = selectedSlot.date;
      if (!reservationMode && recurringEnabled) {
        const recurringBooking = await createRecurringMutation.mutateAsync({
          subFieldId: selectedSubField.id,
          startDate: slotDate,
          endDate: recurringEndDate < slotDate ? slotDate : recurringEndDate,
          intervalDays: recurringIntervalDays,
          startTime: `${selectedStartTime}:00`,
          endTime: end.toTimeString().slice(0, 8),
        });
        const firstBooking = recurringBooking.firstBooking;
        window.location.assign(
          firstBooking
            ? (firstBooking.paymentStatus === "PAID"
                ? `/bookings/${firstBooking.id}`
                : `/bookings/${firstBooking.id}/payment`)
            : "/recurring-bookings",
        );
        return;
      }
      if (reservationMode) {
        const reservation = await createReservationMutation.mutateAsync({
          subFieldId: selectedSubField.id,
          bookingDate: slotDate,
          startTime: `${selectedStartTime}:00`,
          startDateTime: formatLocalDateTime(start),
          endDateTime: formatLocalDateTime(end),
          durationMinutes: effectiveDuration,
          note: note.trim() || undefined,
        });
        window.location.assign(`/owner/fields/${fieldId}/reservations?status=${reservation.status}`);
        return;
      }
      const booking = await createMutation.mutateAsync({
        subFieldId: selectedSubField.id,
        bookingDate: slotDate,
        startTime: `${selectedStartTime}:00`,
        startDateTime: formatLocalDateTime(start),
        endDateTime: formatLocalDateTime(end),
        durationMinutes: effectiveDuration,
        note: note.trim() || undefined,
      });
      window.location.assign(`/bookings/${booking.id}`);
    } catch {
      setIsProcessing(false);
      // React Query exposes the booking conflict below.
    }
  }

  return (
    <form
      onSubmit={submit}
      className="grid gap-7 lg:grid-cols-[minmax(0,1fr)_22rem]"
    >
      <div className="space-y-7 rounded-2xl border border-slate-200 bg-white p-6 shadow-sm sm:p-8">
        <div>
          <p className="text-sm font-black uppercase text-green-600">
            {field.data.name}
          </p>
          <h1 className="mt-2 text-4xl font-black leading-tight text-slate-950">
            Chọn lịch thi đấu
          </h1>
          <p className="mt-3 text-base leading-7 text-slate-600">
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
                  className={`flex items-center justify-between rounded-2xl border p-4 text-left transition ${selectedType === type ? "border-green-500 bg-green-50 ring-2 ring-green-100" : "border-slate-200 hover:border-green-300"}`}
                >
                  <span>
                    <strong className="block text-sm text-slate-900">
                      {formatEnum(type)}
                    </strong>
                    <span className="mt-1 block text-xs text-slate-500">
                      {count} sân đang hoạt động
                    </span>
                  </span>
                  <Layers3 className="size-5 text-green-600" />
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
                className={`rounded-2xl border p-4 text-left transition ${selectedId === subField.id ? "border-green-500 bg-green-50 ring-2 ring-green-100" : "border-slate-200 hover:border-green-300"}`}
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
              <span className="grid size-7 place-items-center rounded-full bg-green-100 text-xs font-black text-green-700">3</span>
              <h2 className="font-black text-slate-900">Bảng giá {selectedSubField.name}</h2>
            </div>
            <div className="overflow-hidden rounded-2xl border border-slate-200">
              {selectedSubField.timePriceRules
              .sort((a, b) => a.startTime.localeCompare(b.startTime))
              .map((rule) => (
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
                  const nextDate = event.target.value;
                  setDate(nextDate);
                  if (recurringEndDate < nextDate) {
                    setRecurringEndDate(addDays(nextDate, 7));
                  }
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
                  key={slot.key}
                  type="button"
                  onClick={() => setSelectedSlotKey(slot.key)}
                  className={`rounded-xl border px-3 py-2.5 text-sm font-bold transition ${selectedSlot?.key === slot.key ? "border-green-600 bg-green-600 text-white" : "border-slate-200 bg-white hover:border-green-300 hover:bg-green-50"}`}
                >
                  <span className="block">{slot.time}</span>
                  {slot.date !== date ? (
                    <span className="mt-0.5 block text-[10px] font-semibold opacity-70">
                      {new Date(`${slot.date}T00:00:00`).toLocaleDateString("vi-VN")}
                    </span>
                  ) : null}
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
            <div className="mt-4 flex gap-3 rounded-2xl border border-green-100 bg-green-50 p-4 text-sm text-green-800">
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
          <label className={`${reservationMode ? "hidden" : "mb-4 flex"} items-start gap-3 rounded-2xl border border-slate-200 p-4`}>
            <input
              type="checkbox"
              checked={recurringEnabled}
              onChange={(event) => setRecurringEnabled(event.target.checked)}
              className="mt-1 size-4"
            />
            <span>
              <strong className="block text-sm text-slate-900">Đặt sân định kỳ</strong>
              <span className="mt-1 block text-xs text-slate-500">
                Đặt sân này cùng một thời gian mỗi tuần. Bạn cần đặt sân này trước ít nhất một lần.
              </span>
            </span>
          </label>
          {!reservationMode && recurringEnabled ? (
            <div className="mb-4 grid gap-4 sm:grid-cols-2">
              <Field label="Lặp lại mỗi">
                <select
                  value={recurringIntervalDays}
                  onChange={(event) => setRecurringIntervalDays(Number(event.target.value))}
                  className="input-field"
                >
                  {[1, 2, 3, 4, 5, 6, 7].map((value) => (
                    <option key={value} value={value}>
                      {value} ngày
                    </option>
                  ))}
                </select>
              </Field>
              <Field label="Ngày đặt sân định kỳ cuối cùng">
                <input
                  type="date"
                  min={date}
                  value={recurringEndDate}
                  onChange={(e) => setRecurringEndDate(e.target.value)}
                  className="input-field"
                />
              </Field>
              <div className="rounded-2xl border border-slate-200 bg-white p-4 text-sm sm:col-span-2">
                <p className="font-bold text-slate-800">Lịch định kỳ sẽ diễn ra vào:</p>
                {recurringPreview.length ? (
                  <p className="mt-2 text-slate-600">
                    {recurringPreview
                      .map((item) =>
                        new Date(`${item}T00:00:00`).toLocaleDateString("vi-VN", {
                          month: "short",
                          day: "numeric",
                        }),
                      )
                      .join(", ")}
                    {recurringPreview.length === 8 ? ", ..." : ""}
                  </p>
                ) : (
                  <p className="mt-2 text-rose-600">Chọn ngày kết thúc bằng hoặc sau ngày bắt đầu.</p>
                )}
              </div>
            </div>
          ) : null}
          <div className="rounded-2xl border border-slate-200 bg-slate-50 p-4 text-sm">
            <div className="flex items-center justify-between">
              <span className="text-slate-500">Phí đặt sân</span>
              <strong className="text-slate-950">
                {estimatedTotalWithFee === null ? "..." : formatCurrency(estimatedTotalWithFee)}
              </strong>
            </div>
            <div className={`${reservationMode ? "hidden" : "mt-3 flex"} items-center justify-between`}>
              <span className="text-slate-500">Số dư ví hiện tại</span>
              <strong className="text-slate-950">
                {walletBalance === null ? "..." : formatCurrency(walletBalance)}
              </strong>
            </div>
            {reservationMode ? null : !isFeeReady ? (
              <p className="mt-3 rounded-xl bg-white/70 p-3 text-slate-500">
                Đang tải thông tin phí.
              </p>
            ) : hasEnoughBalance ? (
              <div className="mt-3 flex items-center justify-between border-t border-slate-200 pt-3">
                <span className="text-slate-500">Số dư sau thanh toán</span>
                <strong className="text-emerald-700">{formatCurrency(remainingBalance)}</strong>
              </div>
            ) : (
              <p className="mt-3 rounded-xl bg-amber-50 p-3 text-amber-700">
                Balance is not enough. Please top up your wallet before creating this booking.
              </p>
            )}
          </div>
        </BookingStep>
      </div>

      <aside className="h-fit rounded-2xl border border-slate-200 bg-white p-6 text-slate-900 shadow-sm lg:sticky lg:top-24">
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
            value={new Date(`${selectedSlot?.date ?? date}T00:00:00`).toLocaleDateString("vi-VN")}
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
          <span className="text-sm text-slate-500">Giá trả tại sân</span>
          <strong className="mt-1 block text-xl">
            {estimatedTotal === null
              ? "Chọn giờ để xem giá"
              : formatCurrency(estimatedTotal)}
          </strong>
        </div>
        <div className="mt-6 border-t border-slate-200 pt-5">
          <span className="text-sm text-slate-500">Phí đặt sân</span>
          <strong className="mt-1 block text-2xl">
            {estimatedTotalWithFee === null
              ? "Chọn giờ để xem giá"
              : formatCurrency(estimatedTotalWithFee)}
          </strong>
        </div>
        {createMutation.error || createRecurringMutation.error ? (
          <p className="mt-4 rounded-xl bg-rose-50 p-3 text-sm text-rose-700">
            {(createMutation.error || createRecurringMutation.error)?.message}
          </p>
        ) : null}
        <button
          disabled={
            !selectedSubField ||
            !selectedStartTime ||
            (!reservationMode && !isFeeReady) ||
            createMutation.isPending ||
            createReservationMutation.isPending ||
            createRecurringMutation.isPending ||
            isProcessing ||
            (!reservationMode && !hasEnoughBalance)
          }
          className="action-button mt-6 w-full bg-green-600 px-5 text-white hover:bg-green-700"
        >
          {createMutation.isPending || createRecurringMutation.isPending ? (
            <LoaderCircle className="size-4 animate-spin" />
          ) : null}
          {recurringEnabled ? "Tạo đặt sân định kỳ" : "Xác nhận đặt sân"}
        </button>
      </aside>
    </form>
  );
}

function formatLocalDateTime(value: Date) {
  const year = value.getFullYear();
  const month = String(value.getMonth() + 1).padStart(2, "0");
  const day = String(value.getDate()).padStart(2, "0");
  const hours = String(value.getHours()).padStart(2, "0");
  const minutes = String(value.getMinutes()).padStart(2, "0");
  const seconds = String(value.getSeconds()).padStart(2, "0");
  return `${year}-${month}-${day}T${hours}:${minutes}:${seconds}`;
}

function buildRecurringPreview(startDate: string, endDate: string, intervalDays: number) {
  if (!startDate || !endDate || intervalDays < 1 || intervalDays > 7 || endDate < startDate) {
    return [];
  }
  const result: string[] = [];
  const cursor = new Date(`${startDate}T00:00:00Z`);
  const end = new Date(`${endDate}T00:00:00Z`);
  while (cursor <= end && result.length < 8) {
    result.push(cursor.toISOString().slice(0, 10));
    cursor.setDate(cursor.getDate() + intervalDays);
  }
  return result;
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
        <span className="grid size-7 place-items-center rounded-full bg-green-100 text-xs font-black text-green-700">
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
      <span className="text-green-600 [&_svg]:size-4">{icon}</span>
      <div>
        <dt className="text-slate-500">{label}</dt>
        <dd className="font-bold text-slate-900">{value}</dd>
      </div>
    </div>
  );
}
