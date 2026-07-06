import { describe, expect, it } from "vitest";
import type { Availability, SubField } from "@/lib/api/types";
import { buildAggregatedSlots, calculateBookingPrice, hidePastSlots } from "./booking-slots";

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
});
