import { afterEach, describe, expect, it, vi } from "vitest";

function jsonResponse(status: number, body: unknown) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}

async function loadRequestJson() {
  vi.resetModules();
  return (await import("./http")).requestJson;
}

afterEach(() => {
  vi.unstubAllGlobals();
  vi.restoreAllMocks();
});

describe("requestJson authentication retry", () => {
  it("refreshes once after a 401 and retries the original request", async () => {
    const fetchMock = vi
      .fn<typeof fetch>()
      .mockResolvedValueOnce(jsonResponse(401, { message: "Expired" }))
      .mockResolvedValueOnce(jsonResponse(200, { success: true }))
      .mockResolvedValueOnce(jsonResponse(200, { id: "user-1" }));
    vi.stubGlobal("fetch", fetchMock);
    const requestJson = await loadRequestJson();

    await expect(requestJson<{ id: string }>("/api/profile")).resolves.toEqual({
      id: "user-1",
    });

    expect(fetchMock).toHaveBeenCalledTimes(3);
    expect(fetchMock.mock.calls[0]?.[0]).toBe("/api/profile");
    expect(fetchMock.mock.calls[1]?.[0]).toBe("/api/auth/refresh");
    expect(fetchMock.mock.calls[2]?.[0]).toBe("/api/profile");
  });

  it("does not recursively refresh authentication endpoints", async () => {
    const fetchMock = vi
      .fn<typeof fetch>()
      .mockResolvedValue(jsonResponse(401, { message: "Invalid OTP" }));
    vi.stubGlobal("fetch", fetchMock);
    const requestJson = await loadRequestJson();

    await expect(
      requestJson("/api/auth/otp/verify", { method: "POST" }),
    ).rejects.toThrow("Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.");
    expect(fetchMock).toHaveBeenCalledTimes(1);
  });

  it("does not retry the request when refreshing fails", async () => {
    const fetchMock = vi
      .fn<typeof fetch>()
      .mockResolvedValueOnce(jsonResponse(401, { message: "Expired" }))
      .mockResolvedValueOnce(jsonResponse(401, { message: "Session expired" }));
    vi.stubGlobal("fetch", fetchMock);
    const requestJson = await loadRequestJson();

    await expect(requestJson("/api/profile")).rejects.toThrow(
      "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.",
    );
    expect(fetchMock).toHaveBeenCalledTimes(2);
  });
});
