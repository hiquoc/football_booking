import { describe, expect, it } from "vitest";
import {
  DEFAULT_AUTH_REDIRECT_PATH,
  isSafeInternalRedirect,
  requestedPathFromUrl,
  safeAuthRedirect,
} from "./auth-redirect";

describe("auth redirects", () => {
  it("captures the pathname and query string from a requested URL", () => {
    expect(requestedPathFromUrl(new URL("https://app.test/fields?page=2&sort=price"))).toBe(
      "/fields?page=2&sort=price",
    );
  });

  it("allows internal application routes", () => {
    expect(isSafeInternalRedirect("/bookings/123")).toBe(true);
    expect(isSafeInternalRedirect("/fields/abc/book?mode=reservation")).toBe(true);
  });

  it("rejects external or executable redirects", () => {
    expect(isSafeInternalRedirect("https://example.com")).toBe(false);
    expect(isSafeInternalRedirect("http://example.com")).toBe(false);
    expect(isSafeInternalRedirect("//example.com")).toBe(false);
    expect(isSafeInternalRedirect("javascript:alert(1)")).toBe(false);
    expect(isSafeInternalRedirect(" /bookings")).toBe(false);
  });

  it("falls back when the redirect is missing or invalid", () => {
    expect(safeAuthRedirect(undefined)).toBe(DEFAULT_AUTH_REDIRECT_PATH);
    expect(safeAuthRedirect("//example.com")).toBe(DEFAULT_AUTH_REDIRECT_PATH);
    expect(safeAuthRedirect("/profile")).toBe("/profile");
  });
});
