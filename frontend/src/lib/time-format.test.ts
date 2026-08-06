import { describe, expect, it } from "vitest";
import {
  clockTimeOptions,
  closingTimeOptions,
  formatTimeLabel,
  requireLocalTimePayload,
  toClosingTimeInputValue,
  toClosingTimePayload,
  toLocalTimePayload,
  toTimeInputValue,
} from "./time-format";

describe("time formatting", () => {
  it("keeps midnight as a valid input and payload value", () => {
    expect(toTimeInputValue("00:00:00")).toBe("00:00");
    expect(toLocalTimePayload("00:00")).toBe("00:00:00");
  });

  it("keeps 23:59 as a valid input and payload value", () => {
    expect(toTimeInputValue("23:59:00")).toBe("23:59");
    expect(toLocalTimePayload("23:59")).toBe("23:59:00");
  });

  it("does not produce malformed values from empty or invalid input", () => {
    expect(toLocalTimePayload("")).toBeNull();
    expect(toLocalTimePayload(":00")).toBeNull();
    expect(requireLocalTimePayload("")).toBe("00:00:00");
  });

  it("maps the closing-time end-of-day alias to 23:59 payload", () => {
    expect(toClosingTimeInputValue("23:59:00")).toBe("23:59");
    expect(toClosingTimeInputValue("23:59")).toBe("23:59");
    expect(toClosingTimePayload("23:59")).toBe("23:59:00");
    expect(closingTimeOptions().at(-1)).toEqual({
      value: "23:59",
      label: "12:00 CH",
    });
  });

  it("formats midnight visually as zero hour", () => {
    expect(clockTimeOptions()[0]).toEqual({ value: "00:00", label: "0:00 SA" });
    expect(formatTimeLabel("00:00:00")).toBe("0:00 SA");
  });
});
