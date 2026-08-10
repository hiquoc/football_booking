import { afterEach, describe, expect, it, vi } from "vitest";
import type {
  CloudinaryUploadResult,
  FieldInput,
  SubFieldInput,
} from "@/lib/api/types";
import {
  submitClosure,
  submitClosureDelete,
  submitClosureUpdate,
  submitField,
  submitFieldUpdate,
  submitImageDelete,
  submitImageOrderChange,
  submitImages,
  submitSubField,
  submitSubFieldDelete,
  submitSubFieldUpdate,
} from "./owner-fields";

function jsonResponse(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}

function requestBody(call: Parameters<typeof fetch>[1]) {
  return JSON.parse(String(call?.body));
}

const fieldInput: FieldInput = {
  name: "District 7 Arena",
  description: "Covered football field",
  address: "12 Nguyen Van Linh",
  ward: "Tan Phong",
  wardCode: "W001",
  province: "Ho Chi Minh City",
  provinceCode: "P001",
  legacyWard: "Tan Phong",
  legacyWardCode: "LW001",
  legacyDistrict: "District 7",
  legacyProvince: "Ho Chi Minh City",
  latitude: 10.729,
  longitude: 106.721,
  phoneNumber: "0900000000",
  email: "owner@example.com",
  active: true,
  operatingHours: [
    {
      dayOfWeek: "MONDAY",
      openTime: "06:00:00",
      closeTime: "23:59:59",
      closed: false,
      open24Hours: false,
    },
    {
      dayOfWeek: "TUESDAY",
      closed: false,
      open24Hours: true,
    },
    {
      dayOfWeek: "WEDNESDAY",
      closed: true,
      open24Hours: false,
    },
  ],
};

const subFieldInput: SubFieldInput = {
  name: "Pitch A",
  description: "5-a-side synthetic turf",
  active: true,
  subFieldType: "FIVE_A_SIDE",
  bookingRule: {
    minimumBookingDurationMinutes: 60,
    maximumBookingDurationMinutes: 180,
    bookingIntervalMinutes: 30,
  },
  timePriceRules: [
    { startTime: "06:00:00", endTime: "17:00:00", hourlyPrice: 180000 },
    { startTime: "17:00:00", endTime: "23:59:59", hourlyPrice: 250000 },
  ],
};

afterEach(() => {
  vi.unstubAllGlobals();
  vi.restoreAllMocks();
});

describe("owner field client requests", () => {
  it("sends field create and update bodies including operating hours", async () => {
    const fetchMock = vi
      .fn<typeof fetch>()
      .mockResolvedValue(jsonResponse({ id: "field-1" }));
    vi.stubGlobal("fetch", fetchMock);

    await submitField(fieldInput);
    await submitFieldUpdate("field-1", fieldInput);

    expect(fetchMock).toHaveBeenNthCalledWith(
      1,
      "/api/owner/fields",
      expect.objectContaining({
        method: "POST",
        credentials: "same-origin",
        cache: "no-store",
      }),
    );
    expect(requestBody(fetchMock.mock.calls[0]![1])).toEqual(fieldInput);
    expect(fetchMock).toHaveBeenNthCalledWith(
      2,
      "/api/owner/fields/field-1",
      expect.objectContaining({ method: "PUT" }),
    );
    expect(requestBody(fetchMock.mock.calls[1]![1])).toEqual(fieldInput);
  });

  it("sends subfield create, update, and delete requests", async () => {
    const fetchMock = vi
      .fn<typeof fetch>()
      .mockResolvedValue(jsonResponse({ id: "sub-1" }));
    vi.stubGlobal("fetch", fetchMock);

    await submitSubField("field-1", subFieldInput);
    await submitSubFieldUpdate("field-1", "sub-1", subFieldInput);
    await submitSubFieldDelete("field-1", "sub-1");

    expect(fetchMock.mock.calls[0]?.[0]).toBe(
      "/api/owner/fields/field-1/sub-fields",
    );
    expect(fetchMock.mock.calls[0]?.[1]?.method).toBe("POST");
    expect(requestBody(fetchMock.mock.calls[0]![1])).toEqual(subFieldInput);
    expect(fetchMock.mock.calls[1]?.[0]).toBe(
      "/api/owner/fields/field-1/sub-fields/sub-1",
    );
    expect(fetchMock.mock.calls[1]?.[1]?.method).toBe("PUT");
    expect(requestBody(fetchMock.mock.calls[1]![1])).toEqual(subFieldInput);
    expect(fetchMock.mock.calls[2]?.[0]).toBe(
      "/api/owner/fields/field-1/sub-fields/sub-1",
    );
    expect(fetchMock.mock.calls[2]?.[1]?.method).toBe("DELETE");
  });

  it("sends closure create, update, and delete requests", async () => {
    const fetchMock = vi
      .fn<typeof fetch>()
      .mockResolvedValue(jsonResponse([{ id: "closure-1" }]));
    vi.stubGlobal("fetch", fetchMock);
    const closureInput = {
      subFieldIds: ["sub-1", "sub-2"],
      startDate: "2026-09-01",
      endDate: "2026-09-03",
      reason: "Maintenance",
    };

    await submitClosure("field-1", closureInput);
    await submitClosureUpdate("field-1", "closure-1", {
      ...closureInput,
      subFieldIds: ["sub-1"],
    });
    await submitClosureDelete("field-1", "closure-1");

    expect(fetchMock.mock.calls[0]?.[0]).toBe(
      "/api/owner/fields/field-1/closures",
    );
    expect(fetchMock.mock.calls[0]?.[1]?.method).toBe("POST");
    expect(requestBody(fetchMock.mock.calls[0]![1])).toEqual(closureInput);
    expect(fetchMock.mock.calls[1]?.[0]).toBe(
      "/api/owner/fields/field-1/closures/closure-1",
    );
    expect(fetchMock.mock.calls[1]?.[1]?.method).toBe("PUT");
    expect(requestBody(fetchMock.mock.calls[1]![1])).toEqual({
      ...closureInput,
      subFieldIds: ["sub-1"],
    });
    expect(fetchMock.mock.calls[2]?.[0]).toBe(
      "/api/owner/fields/field-1/closures/closure-1",
    );
    expect(fetchMock.mock.calls[2]?.[1]?.method).toBe("DELETE");
  });

  it("requests image upload slots, uploads files, and confirms Cloudinary results", async () => {
    vi.stubGlobal("crypto", { randomUUID: () => "request-1" });
    const cloudinaryResult: CloudinaryUploadResult = {
      public_id: "fields/field-1/image-1",
      secure_url: "https://cdn.example.com/image-1.webp",
      version: 123,
      signature: "cloudinary-signature",
      format: "webp",
      width: 1200,
      height: 800,
      bytes: 456789,
    };
    const fetchMock = vi
      .fn<typeof fetch>()
      .mockResolvedValueOnce(
        jsonResponse([
          {
            imageId: 10,
            publicId: "fields/field-1/image-1",
            timestamp: 123456,
            signature: "slot-signature",
            apiKey: "api-key",
            cloudName: "cloud",
            uploadUrl: "https://api.cloudinary.test/upload",
            overwrite: false,
          },
        ]),
      )
      .mockResolvedValueOnce(jsonResponse(cloudinaryResult))
      .mockResolvedValueOnce(jsonResponse([{ id: 10 }]));
    vi.stubGlobal("fetch", fetchMock);
    const files = [
      new File(["image-data"], "pitch.webp", { type: "image/webp" }),
    ] as unknown as FileList;

    await submitImages("field-1", files);

    expect(fetchMock.mock.calls[0]?.[0]).toBe(
      "/api/owner/fields/field-1/images/upload-slots",
    );
    expect(requestBody(fetchMock.mock.calls[0]![1])).toEqual({
      requestId: "request-1",
      count: 1,
    });
    expect(fetchMock.mock.calls[1]?.[0]).toBe(
      "https://api.cloudinary.test/upload",
    );
    expect(fetchMock.mock.calls[1]?.[1]?.body).toBeInstanceOf(FormData);
    expect(fetchMock.mock.calls[2]?.[0]).toBe(
      "/api/owner/fields/field-1/images/confirm",
    );
    expect(requestBody(fetchMock.mock.calls[2]![1])).toEqual([
      cloudinaryResult,
    ]);
  });

  it("sends image delete and order update requests", async () => {
    const fetchMock = vi
      .fn<typeof fetch>()
      .mockResolvedValue(jsonResponse({ success: true }));
    vi.stubGlobal("fetch", fetchMock);

    await submitImageDelete("field-1", 10);
    await submitImageOrderChange("field-1", [12, 10, 11]);

    expect(fetchMock.mock.calls[0]?.[0]).toBe(
      "/api/owner/fields/field-1/images/10",
    );
    expect(fetchMock.mock.calls[0]?.[1]?.method).toBe("DELETE");
    expect(fetchMock.mock.calls[1]?.[0]).toBe(
      "/api/owner/fields/field-1/images/order",
    );
    expect(fetchMock.mock.calls[1]?.[1]?.method).toBe("PUT");
    expect(requestBody(fetchMock.mock.calls[1]![1])).toEqual({
      imageIds: [12, 10, 11],
    });
  });
});
