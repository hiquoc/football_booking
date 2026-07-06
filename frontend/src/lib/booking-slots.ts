import type { Availability, SubField } from "@/lib/api/types";
import { formatTime } from "./field-format";

function toMinutes(value: string) {
  const [hours, minutes] = value.split(":").map(Number);
  return hours * 60 + minutes;
}

function fromMinutes(value: number) {
  return `${String(Math.floor(value / 60)).padStart(2, "0")}:${String(value % 60).padStart(2, "0")}`;
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
) {
  if (!availability) return [];
  const open = toMinutes(availability.openTime);
  const close = toMinutes(availability.closeTime);
  const unavailable = availability.unavailableSlots.map((slot) => [
    toMinutes(slot.startTime),
    toMinutes(slot.endTime),
  ]);
  const slots: string[] = [];

  for (let start = open; start + duration <= close; start += interval) {
    const end = start + duration;
    if (
      !unavailable.some(
        ([blockedStart, blockedEnd]) =>
          start < blockedEnd && end > blockedStart,
      )
    )
      slots.push(fromMinutes(start));
  }
  return slots;
}

export function hidePastSlots(
  slots: string[],
  bookingDate: string,
  now: Date,
) {
  if (bookingDate !== toLocalDate(now)) return slots;

  const currentMinutes = now.getHours() * 60 + now.getMinutes();
  return slots.filter((slot) => toMinutes(slot) > currentMinutes);
}

export interface AggregatedBookingSlot {
  time: string;
  subFields: SubField[];
}

export function buildAggregatedSlots(
  subFields: SubField[],
  availabilities: Array<Availability | undefined>,
  duration: number,
  interval: number,
): AggregatedBookingSlot[] {
  const slots = new Map<string, SubField[]>();

  subFields.forEach((subField, index) => {
    const rule = subField.bookingRule;
    if (
      rule &&
      (duration < rule.minimumBookingDurationMinutes ||
        duration > rule.maximumBookingDurationMinutes)
    ) {
      return;
    }

    buildAvailableSlots(availabilities[index], duration, interval).forEach(
      (time) => {
        slots.set(time, [...(slots.get(time) ?? []), subField]);
      },
    );
  });

  return [...slots.entries()]
    .map(([time, matchingSubFields]) => ({
      time,
      subFields: matchingSubFields.sort(
        (left, right) =>
          (priceAt(left, time) ?? Number.MAX_SAFE_INTEGER) -
          (priceAt(right, time) ?? Number.MAX_SAFE_INTEGER),
      ),
    }))
    .sort((left, right) => left.time.localeCompare(right.time));
}

export function priceAt(subField: SubField | undefined, startTime: string) {
  if (!subField) return null;
  const rule = subField.timePriceRules.find(
    (priceRule) =>
      startTime >= formatTime(priceRule.startTime) &&
      startTime < formatTime(priceRule.endTime),
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
    const overlapStart = Math.max(bookingStart, toMinutes(formatTime(rule.startTime)));
    const overlapEnd = Math.min(bookingEnd, toMinutes(formatTime(rule.endTime)));
    if (overlapEnd <= overlapStart) continue;
    const minutes = overlapEnd - overlapStart;
    coveredMinutes += minutes;
    total += (Number(rule.hourlyPrice) * minutes) / 60;
  }

  if (coveredMinutes !== durationMinutes) return null;
  return Math.ceil(total / 1000) * 1000;
}
