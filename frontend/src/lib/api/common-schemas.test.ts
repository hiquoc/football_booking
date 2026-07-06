import { describe, expect, it } from "vitest";
import { uuidSchema } from "./common-schemas";

describe("uuidSchema", () => {
  it("accepts deterministic PostgreSQL UUID values without an RFC version nibble", () => {
    expect(uuidSchema.parse("01234567-89ab-cdef-0123-456789abcdef"))
      .toBe("01234567-89ab-cdef-0123-456789abcdef");
  });

  it("rejects malformed identifiers", () => {
    expect(uuidSchema.safeParse("not-a-uuid").success).toBe(false);
  });
});
