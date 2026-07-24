import { describe, expect, it } from "vitest";
import { fieldInputSchema } from "./field-schema";

const baseInput = {
  name: "ABC Football Center",
  description: "",
  address: "123 Nguyen Hue",
  ward: "Phuong Sai Gon",
  wardCode: "26743",
  province: "Thanh pho Ho Chi Minh",
  provinceCode: "79",
  legacyWard: "Phuong Ben Nghe",
  legacyWardCode: "26743",
  legacyDistrict: "Quan 1",
  legacyProvince: "Thanh pho Ho Chi Minh",
  latitude: 10.7769,
  longitude: 106.7009,
  phoneNumber: "0862470050",
  email: "",
  active: true,
};

const days = [
  "MONDAY",
  "TUESDAY",
  "WEDNESDAY",
  "THURSDAY",
  "FRIDAY",
  "SATURDAY",
  "SUNDAY",
];

describe("fieldInputSchema operating hours", () => {
  it("accepts midnight and end-of-day LocalTime strings", () => {
    const input = {
      ...baseInput,
      operatingHours: days.map((dayOfWeek) => ({
        dayOfWeek,
        openTime: "00:00:00",
        closeTime: "23:59:00",
        closed: false,
        open24Hours: false,
      })),
    };

    expect(fieldInputSchema.parse(input).operatingHours[0]).toMatchObject({
      openTime: "00:00:00",
      closeTime: "23:59:00",
    });
  });

  it("accepts open 24 hours without time values", () => {
    const input = {
      ...baseInput,
      operatingHours: days.map((dayOfWeek) => ({
        dayOfWeek,
        closed: false,
        open24Hours: true,
      })),
    };

    expect(fieldInputSchema.parse(input).operatingHours[0]).toMatchObject({
      open24Hours: true,
    });
  });

  it("rejects malformed time values", () => {
    const input = {
      ...baseInput,
      operatingHours: days.map((dayOfWeek) => ({
        dayOfWeek,
        openTime: ":00",
        closeTime: "23:00:00",
        closed: false,
      })),
    };

    expect(() => fieldInputSchema.parse(input)).toThrow();
  });
});
