import { describe, expect, it } from "vitest";
import { sendOtpSchema, verifyOtpSchema } from "./auth-schemas";

describe("authentication schemas", () => {
  it.each(["0862470050", "+84912345678"])("accepts %s", (phoneNumber) => {
    expect(sendOtpSchema.safeParse({ phoneNumber }).success).toBe(true);
  });

  it("rejects malformed OTP input", () => {
    expect(
      verifyOtpSchema.safeParse({ phoneNumber: "0862470050", code: "123" })
        .success,
    ).toBe(false);
  });
});
