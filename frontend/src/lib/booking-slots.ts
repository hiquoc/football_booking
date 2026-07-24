import type { Availability, SubField } from "@/lib/api/types";
import { formatTime } from "./field-format";

const END_OF_DAY_MINUTES = 23 * 60 + 59;
const MIDNIGHT_MINUTES = 24 * 60;
const START_OF_DAY_STRING = "00:00";

function toMinutes(value: string) {
  const [hours, minutes] = value.split(":").map(Number);
  return hours * 60 + minutes;
}

function fromMinutes(value: number) {
  const wrapped = ((value % 1440) + 1440) % 1440;
  return `${String(Math.floor(wrapped / 60)).padStart(2, "0")}:${String(wrapped % 60).padStart(2, "0")}`;
}

function addDays(dateString: string, days: number) {
  const value = new Date(`${dateString}T00:00:00`);
  value.setDate(value.getDate() + days);
  return toLocalDate(value);
}

function toLocalDate(value: Date) {
  const year = value.getFullYear();
  const month = String(value.getMonth() + 1).padStart(2, "0");
  const day = String(value.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}

export function buildAvailableSlots(
  availability: Availability | undefined,
  duration: number,
  interval: number,
  bookingDate?: string,
) {
  return buildAvailableSlotOptions(availability, duration, interval, bookingDate).map((slot) => slot.time);
}

export interface AvailableSlotOption {
  key: string;
  time: string;
  date: string;
  startDateTime: string;
  relativeMinutes: number;
}

export function buildAvailableSlotOptions(
  availability: Availability | undefined,
  duration: number,
  interval: number,
  bookingDate?: string,
) {
  if (!availability) return [];
  const timelineDate = bookingDate ?? toLocalDate(new Date());
  const intervals = buildOpenIntervals(availability, timelineDate);
  const activeInterval = intervals.find((interval) => interval.end > 0 && interval.start < 1440);
  if (!activeInterval) return [];
  const unavailable = availability.unavailableSlots.map((slot) => [
    slot.startDateTime ? toRelativeMinutes(slot.startDateTime, timelineDate) : normalizeSlotMinute(toMinutes(slot.startTime), activeInterval.start),
    slot.endDateTime ? toRelativeMinutes(slot.endDateTime, timelineDate) : normalizeSlotMinute(toMinutes(slot.endTime), activeInterval.start),
  ]);
  const slots: AvailableSlotOption[] = [];
  const firstStart = alignStart(Math.max(0, activeInterval.start), activeInterval.start, interval);
  const lastStart = Math.min(activeInterval.end - duration, MIDNIGHT_MINUTES - 1);

  for (let start = firstStart; start <= lastStart; start += interval) {
    const end = start + duration;
    if (
      !unavailable.some(
        ([blockedStart, blockedEnd]) =>
          start < blockedEnd && end > blockedStart,
      )
    )
      slots.push(toSlotOption(timelineDate, start));
  }
  return slots;
}

function buildOpenIntervals(availability: Availability, bookingDate: string) {
  const schedules = availability.operatingHours?.length
    ? availability.operatingHours
    : [{
      date: bookingDate,
      openTime: availability.openTime,
      closeTime: availability.closeTime,
      closed: !availability.open24Hours && (!availability.openTime || !availability.closeTime),
      open24Hours: availability.open24Hours,
    }];

  const scheduleByDate = new Map(schedules.map((schedule) => [schedule.date, schedule]));
  const raw = schedules.flatMap((schedule) => {
    if (schedule.closed) return [];
    const dayOffset = daysBetween(bookingDate, schedule.date);
    if (schedule.open24Hours) {
      return [{ start: dayOffset * 1440, end: (dayOffset + 1) * 1440 }];
    }
    if (!schedule.openTime || !schedule.closeTime) return [];
    const open = dayOffset * 1440 + toMinutes(schedule.openTime);
    let closeMinute = toMinutes(schedule.closeTime);
    if (closeMinute === END_OF_DAY_MINUTES && opensAtMidnight(scheduleByDate.get(addDays(schedule.date, 1)))) {
      closeMinute = MIDNIGHT_MINUTES;
    }
    let close = dayOffset * 1440 + closeMinute;
    if (close <= open) close += 1440;
    return [{ start: open, end: close }];
  }).sort((left, right) => left.start - right.start);

  return raw.reduce<Array<{ start: number; end: number }>>((merged, interval) => {
    const previous = merged[merged.length - 1];
    if (previous && interval.start <= previous.end) {
      previous.end = Math.max(previous.end, interval.end);
    } else {
      merged.push({ ...interval });
    }
    return merged;
  }, []);
}

function opensAtMidnight(schedule: NonNullable<Availability["operatingHours"]>[number] | undefined) {
  if (!schedule || schedule.closed) return false;
  return schedule.open24Hours || schedule.openTime === "00:00" || schedule.openTime === "00:00:00";
}

function daysBetween(baseDate: string, value: string) {
  const base = new Date(`${baseDate}T00:00:00`).getTime();
  const current = new Date(`${value}T00:00:00`).getTime();
  return Math.round((current - base) / 86_400_000);
}

function alignStart(lowerBound: number, intervalStart: number, interval: number) {
  if (lowerBound <= intervalStart) return intervalStart;
  return intervalStart + Math.ceil((lowerBound - intervalStart) / interval) * interval;
}

function toSlotOption(bookingDate: string, relativeMinutes: number): AvailableSlotOption {
  const dayOffset = Math.floor(relativeMinutes / 1440);
  const date = addDays(bookingDate, dayOffset);
  const time = fromMinutes(relativeMinutes);
  return {
    key: `${date}T${time}`,
    time,
    date,
    startDateTime: `${date}T${time}:00`,
    relativeMinutes,
  };
}

function normalizeSlotMinute(value: number, open: number) {
  return value < open ? value + 1440 : value;
}

function toRelativeMinutes(dateTime: string, bookingDate: string) {
  const value = new Date(dateTime);
  const base = new Date(`${bookingDate}T00:00:00`);
  return Math.round((value.getTime() - base.getTime()) / 60_000);
}

export function hidePastSlots(
  slots: string[],
  bookingDate: string,
  now: Date,
  availability?: Availability,
) {
  if (bookingDate !== toLocalDate(now)) return slots;

  const currentMinutes = now.getHours() * 60 + now.getMinutes();
  const overnightClose = availability?.openTime && availability.closeTime && toMinutes(availability.closeTime) <= toMinutes(availability.openTime)
    ? toMinutes(availability.closeTime)
    : null;
  return slots.filter((slot) => {
    const slotMinutes = toMinutes(slot);
    return slotMinutes > currentMinutes || (overnightClose !== null && slotMinutes < overnightClose);
  });
}

export function hidePastSlotOptions(
  slots: AvailableSlotOption[],
  bookingDate: string,
  now: Date,
) {
  return slots.filter((slot) => new Date(slot.startDateTime) > now || slot.date !== bookingDate);
}

export interface AggregatedBookingSlot {
  time: string;
  date: string;
  startDateTime: string;
  relativeMinutes: number;
  subFields: SubField[];
}

export function buildAggregatedSlots(
  subFields: SubField[],
  availabilities: Array<Availability | undefined>,
  duration: number,
  interval: number,
  bookingDate?: string,
): AggregatedBookingSlot[] {
  const slots = new Map<string, { slot: AvailableSlotOption; subFields: SubField[] }>();

  subFields.forEach((subField, index) => {
    const rule = subField.bookingRule;
    if (
      rule &&
      (duration < rule.minimumBookingDurationMinutes ||
        duration > rule.maximumBookingDurationMinutes)
    ) {
      return;
    }

    buildAvailableSlotOptions(availabilities[index], duration, interval, bookingDate).forEach(
      (slot) => {
        const existing = slots.get(slot.key);
        slots.set(slot.key, {
          slot,
          subFields: [...(existing?.subFields ?? []), subField],
        });
      },
    );
  });

  return [...slots.values()]
    .map(({ slot, subFields }) => ({
      time: slot.time,
      date: slot.date,
      startDateTime: slot.startDateTime,
      relativeMinutes: slot.relativeMinutes,
      subFields: subFields.sort(
        (left, right) =>
          (priceAt(left, slot.time) ?? Number.MAX_SAFE_INTEGER) -
          (priceAt(right, slot.time) ?? Number.MAX_SAFE_INTEGER),
      ),
    }))
    .sort((left, right) => left.relativeMinutes - right.relativeMinutes);
}

export function priceAt(subField: SubField | undefined, startTime: string) {
  if (!subField) return null;
  const rule = subField.timePriceRules.find(
    (priceRule) => isWithinPriceRule(toMinutes(startTime), priceRule),
  );
  return rule ? Number(rule.hourlyPrice) : null;
}

export function calculateBookingPrice(
  subField: SubField | undefined,
  startTime: string,
  durationMinutes: number,
) {
  if (!subField || !startTime || durationMinutes <= 0) return null;
  const bookingStart = toMinutes(startTime);
  const bookingEnd = bookingStart + durationMinutes;
  let coveredMinutes = 0;
  let total = 0;

  for (const rule of subField.timePriceRules) {
    for (const [ruleStart, ruleEnd] of priceRuleWindowsFor(rule)) {
      const overlapStart = Math.max(bookingStart, ruleStart);
      const overlapEnd = Math.min(bookingEnd, ruleEnd);
      if (overlapEnd <= overlapStart) continue;
      const minutes = overlapEnd - overlapStart;
      coveredMinutes += minutes;
      total += (Number(rule.hourlyPrice) * minutes) / 60;
    }
  }

  if (coveredMinutes !== durationMinutes) return null;
  return Math.ceil(total / 1000) * 1000;
}

function isWithinPriceRule(time: number, rule: SubField["timePriceRules"][number]) {
  const start = toMinutes(formatTime(rule.startTime));
  const end = priceRuleEndMinute(rule);
  if (end > start) return time >= start && time < end;
  return time >= start || time < end;
}

function priceRuleWindowsFor(rule: SubField["timePriceRules"][number]) {
  const start = toMinutes(formatTime(rule.startTime));
  let end = priceRuleEndMinute(rule);
  if (end <= start) end += MIDNIGHT_MINUTES;
  return [-MIDNIGHT_MINUTES, 0, MIDNIGHT_MINUTES].map((offset) => (
    [start + offset, end + offset] as const
  ));
}

function priceRuleEndMinute(rule: SubField["timePriceRules"][number]) {
  const end = toMinutes(formatTime(rule.endTime));
  return end === END_OF_DAY_MINUTES ? MIDNIGHT_MINUTES : end;
}
