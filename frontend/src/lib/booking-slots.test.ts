import { describe, expect, it } from "vitest";
import type { Availability, SubField } from "@/lib/api/types";
import {
  buildAggregatedSlots,
  buildAvailableSlotOptions,
  calculateBookingPrice,
  hidePastSlots,
} from "./booking-slots";

function subField(id: string, hourlyPrice: number): SubField {
  return {
    id,
    fieldId: "field-id",
    fieldType: "FOOTBALL",
    name: `Sân ${id}`,
    description: null,
    active: true,
    bookingDisabledFrom: null,
    indoorOutdoor: "OUTDOOR",
    surfaceType: "ARTIFICIAL_GRASS",
    subFieldType: "FOOTBALL_5V5",
    maxPlayers: 10,
    lighting: true,
    parking: false,
    changingRoom: false,
    shower: false,
    wifi: false,
    airConditioning: false,
    bookingRule: {
      id: 1,
      minimumBookingDurationMinutes: 60,
      maximumBookingDurationMinutes: 120,
      bookingIntervalMinutes: 60,
    },
    timePriceRules: [
      { id: 1, startTime: "06:00:00", endTime: "08:00:00", hourlyPrice },
    ],
    createdAt: "2026-01-01T00:00:00",
    updatedAt: "2026-01-01T00:00:00",
  };
}

const openMorning: Availability = {
  openTime: "06:00:00",
  closeTime: "08:00:00",
  open24Hours: false,
  unavailableSlots: [],
};

describe("buildAggregatedSlots", () => {
  it("groups available sub-fields by time and keeps partially available slots", () => {
    const first = subField("A", 200_000);
    const second = subField("B", 250_000);
    const slots = buildAggregatedSlots(
      [first, second],
      [
        {
          ...openMorning,
          unavailableSlots: [{ startTime: "06:00:00", endTime: "07:00:00" }],
        },
        openMorning,
      ],
      60,
      60,
    );

    expect(slots.map((slot) => [slot.time, slot.subFields.length])).toEqual([
      ["06:00", 1],
      ["07:00", 2],
    ]);
    expect(slots[1].subFields[0].id).toBe("A");
  });

  it("uses next-day operating hours when grouping aggregated availability", () => {
    const slots = buildAggregatedSlots(
      [subField("A", 200_000)],
      [{
        openTime: "06:00:00",
        closeTime: "00:00:00",
        open24Hours: false,
        operatingHours: [
          { date: "2026-07-05", openTime: "06:00:00", closeTime: "00:00:00", closed: false, open24Hours: false },
          { date: "2026-07-06", openTime: null, closeTime: null, closed: false, open24Hours: true },
        ],
        unavailableSlots: [],
      }],
      90,
      90,
      "2026-07-05",
    );

    expect(slots.map((slot) => slot.time).slice(10, 12)).toEqual([
      "21:00",
      "22:30",
    ]);
    expect(slots).not.toContainEqual(expect.objectContaining({ date: "2026-07-06", time: "00:00" }));
  });
});

describe("buildAvailableSlotOptions", () => {
  it("uses next-day continuity without returning starts from the next date", () => {
    const slots = buildAvailableSlotOptions({
      openTime: "06:00:00",
      closeTime: "00:00:00",
      open24Hours: false,
      operatingHours: [
        { date: "2026-07-05", openTime: "06:00:00", closeTime: "00:00:00", closed: false, open24Hours: false },
        { date: "2026-07-06", openTime: null, closeTime: null, closed: false, open24Hours: true },
      ],
      unavailableSlots: [],
    }, 90, 90, "2026-07-05");

    expect(slots.map((slot) => [slot.date, slot.time]).slice(10, 12)).toEqual([
      ["2026-07-05", "21:00"],
      ["2026-07-05", "22:30"],
    ]);
    expect(slots).not.toContainEqual(expect.objectContaining({ date: "2026-07-06", time: "00:00" }));
  });

  it("uses the next day opening time to allow late starts ending at midnight", () => {
    const slots = buildAvailableSlotOptions({
      openTime: "06:00:00",
      closeTime: "23:59:00",
      open24Hours: false,
      operatingHours: [
        { date: "2026-07-05", openTime: "06:00:00", closeTime: "23:59:00", closed: false, open24Hours: false },
        { date: "2026-07-06", openTime: "00:00:00", closeTime: "22:00:00", closed: false, open24Hours: false },
      ],
      unavailableSlots: [],
    }, 90, 90, "2026-07-05");

    expect(slots.map((slot) => [slot.date, slot.time]).slice(10, 12)).toEqual([
      ["2026-07-05", "21:00"],
      ["2026-07-05", "22:30"],
    ]);
    expect(slots).not.toContainEqual(expect.objectContaining({ date: "2026-07-06", time: "00:00" }));
  });

  it("does not allow a late midnight-ending start when the next day has a closed gap", () => {
    const slots = buildAvailableSlotOptions({
      openTime: "06:00:00",
      closeTime: "23:59:00",
      open24Hours: false,
      operatingHours: [
        { date: "2026-07-05", openTime: "06:00:00", closeTime: "23:59:00", closed: false, open24Hours: false },
        { date: "2026-07-06", openTime: "06:00:00", closeTime: "22:00:00", closed: false, open24Hours: false },
      ],
      unavailableSlots: [],
    }, 90, 90, "2026-07-05");

    expect(slots.at(-1)).toMatchObject({ date: "2026-07-05", time: "21:00" });
  });

  it("stops at midnight when the next day opens after a closed gap", () => {
    const slots = buildAvailableSlotOptions({
      openTime: "06:00:00",
      closeTime: "00:00:00",
      open24Hours: false,
      operatingHours: [
        { date: "2026-07-05", openTime: "06:00:00", closeTime: "00:00:00", closed: false, open24Hours: false },
        { date: "2026-07-06", openTime: "06:00:00", closeTime: "22:00:00", closed: false, open24Hours: false },
      ],
      unavailableSlots: [],
    }, 90, 90, "2026-07-05");

    expect(slots.at(-1)).toMatchObject({ date: "2026-07-05", time: "22:30" });
  });

  it("shows carry-over slots from a previous overnight opening", () => {
    const slots = buildAvailableSlotOptions({
      openTime: "06:00:00",
      closeTime: "22:00:00",
      open24Hours: false,
      operatingHours: [
        { date: "2026-07-04", openTime: "18:00:00", closeTime: "02:00:00", closed: false, open24Hours: false },
        { date: "2026-07-05", openTime: "06:00:00", closeTime: "22:00:00", closed: false, open24Hours: false },
      ],
      unavailableSlots: [],
    }, 60, 30, "2026-07-05");

    expect(slots.slice(0, 3).map((slot) => slot.time)).toEqual(["00:00", "00:30", "01:00"]);
  });

  it("excludes booked ranges that cross midnight", () => {
    const slots = buildAvailableSlotOptions({
      openTime: "18:00:00",
      closeTime: "02:00:00",
      open24Hours: false,
      operatingHours: [
        { date: "2026-07-05", openTime: "18:00:00", closeTime: "02:00:00", closed: false, open24Hours: false },
      ],
      unavailableSlots: [{
        startTime: "23:00:00",
        endTime: "00:30:00",
        startDateTime: "2026-07-05T23:00:00",
        endDateTime: "2026-07-06T00:30:00",
      }],
    }, 60, 30, "2026-07-05");

    expect(slots.map((slot) => slot.time)).not.toContain("22:30");
    expect(slots.map((slot) => slot.time)).not.toContain("23:00");
    expect(slots.map((slot) => slot.time)).not.toContain("23:30");
    expect(slots.map((slot) => slot.time)).not.toContain("00:00");
    expect(slots.map((slot) => slot.time)).not.toContain("00:30");
  });

  it("excludes previous-day bookings that overlap today's carry-over hours", () => {
    const slots = buildAvailableSlotOptions({
      openTime: "06:00:00",
      closeTime: "22:00:00",
      open24Hours: false,
      operatingHours: [
        { date: "2026-07-04", openTime: "18:00:00", closeTime: "02:00:00", closed: false, open24Hours: false },
        { date: "2026-07-05", openTime: "06:00:00", closeTime: "22:00:00", closed: false, open24Hours: false },
      ],
      unavailableSlots: [{
        startTime: "23:30:00",
        endTime: "00:30:00",
        startDateTime: "2026-07-04T23:30:00",
        endDateTime: "2026-07-05T00:30:00",
      }],
    }, 60, 30, "2026-07-05");

    expect(slots.map((slot) => slot.time)).not.toContain("00:00");
    expect(slots.map((slot) => slot.time)).toContain("00:30");
  });
});

describe("hidePastSlots", () => {
  const slots = ["06:00", "12:00", "12:30", "13:00"];

  it("hides elapsed start times for today", () => {
    const now = new Date(2026, 5, 29, 12, 15);

    expect(hidePastSlots(slots, "2026-06-29", now)).toEqual([
      "12:30",
      "13:00",
    ]);
  });

  it("keeps all start times for a future date", () => {
    const now = new Date(2026, 5, 29, 12, 15);

    expect(hidePastSlots(slots, "2026-06-30", now)).toEqual(slots);
  });
});

describe("calculateBookingPrice", () => {
  it("splits the total across adjacent price rules", () => {
    const field = subField("A", 220_000);
    field.timePriceRules = [
      { id: 1, startTime: "08:00:00", endTime: "17:00:00", hourlyPrice: 220_000 },
      { id: 2, startTime: "17:00:00", endTime: "23:00:00", hourlyPrice: 250_000 },
    ];

    expect(calculateBookingPrice(field, "16:00", 90)).toBe(345_000);
  });

  it("returns null when rules do not cover the complete duration", () => {
    expect(calculateBookingPrice(subField("A", 220_000), "07:30", 90)).toBeNull();
  });

  it("prices bookings covered by an overnight rule", () => {
    const field = subField("A", 180_000);
    field.timePriceRules = [
      { id: 1, startTime: "18:00:00", endTime: "02:00:00", hourlyPrice: 180_000 },
    ];

    expect(calculateBookingPrice(field, "22:30", 210)).toBe(630_000);
    expect(calculateBookingPrice(field, "00:30", 60)).toBe(180_000);
  });

  it("treats 23:59 rule end as end of day for pricing display", () => {
    const field = subField("A", 150_000);
    field.timePriceRules = [
      { id: 1, startTime: "06:00:00", endTime: "17:00:00", hourlyPrice: 150_000 },
      { id: 2, startTime: "17:00:00", endTime: "23:59:00", hourlyPrice: 170_000 },
    ];

    expect(calculateBookingPrice(field, "23:00", 60)).toBe(170_000);
  });

  it("prices bookings that continue into next-day price rules", () => {
    const field = subField("A", 150_000);
    field.timePriceRules = [
      { id: 1, startTime: "17:00:00", endTime: "23:59:00", hourlyPrice: 170_000 },
      { id: 2, startTime: "00:00:00", endTime: "06:00:00", hourlyPrice: 120_000 },
    ];

    expect(calculateBookingPrice(field, "23:30", 90)).toBe(205_000);
  });

  it("splits an end-of-day booking across the final price rule", () => {
    const field = subField("A", 150_000);
    field.timePriceRules = [
      { id: 1, startTime: "06:00:00", endTime: "17:00:00", hourlyPrice: 150_000 },
      { id: 2, startTime: "17:00:00", endTime: "23:59:00", hourlyPrice: 170_000 },
    ];

    expect(calculateBookingPrice(field, "16:30", 90)).toBe(245_000);
  });
});
