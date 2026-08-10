import { afterEach, describe, expect, it, vi } from "vitest";
import type { CloudinaryUploadResult, FieldInput, SubFieldInput } from "@/lib/api/types";

const gatewayRequestMock = vi.fn();

vi.mock("server-only", () => ({}));

vi.mock("next/cache", () => ({
  revalidateTag: vi.fn(),
  unstable_cache: (callback: unknown) => callback,
}));

vi.mock("./authenticated-gateway", () => ({
  authenticatedGatewayRequest: gatewayRequestMock,
  sessionGatewayRequest: vi.fn(),
}));

vi.mock("./gateway", () => ({
  gatewayRequest: vi.fn(),
}));

vi.mock("./session", () => ({
  getAccessToken: vi.fn(),
}));

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
      dayOfWeek: "SUNDAY",
      closed: false,
      open24Hours: true,
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

function bodyAt(index: number) {
  return JSON.parse(String(gatewayRequestMock.mock.calls[index]?.[1]?.body));
}

afterEach(() => {
  gatewayRequestMock.mockReset();
});

describe("owner field backend requests", () => {
  it("sends field create and update requests with operating hours unchanged", async () => {
    gatewayRequestMock.mockResolvedValue({ id: "field-1" });
    const { createField, updateField } = await import("./fields");

    await createField(fieldInput);
    await updateField("field-1", fieldInput);

    expect(gatewayRequestMock).toHaveBeenNthCalledWith(
      1,
      "/api/v1/fields",
      expect.objectContaining({ method: "POST" }),
    );
    expect(bodyAt(0)).toEqual(fieldInput);
    expect(gatewayRequestMock).toHaveBeenNthCalledWith(
      2,
      "/api/v1/fields/field-1",
      expect.objectContaining({ method: "PUT" }),
    );
    expect(bodyAt(1)).toEqual(fieldInput);
  });

  it("sends subfield create, update, and delete requests", async () => {
    gatewayRequestMock.mockResolvedValue({ id: "sub-1" });
    const { createSubField, deleteSubField, updateSubField } = await import("./fields");

    await createSubField("field-1", subFieldInput);
    await updateSubField("field-1", "sub-1", subFieldInput);
    await deleteSubField("field-1", "sub-1");

    expect(gatewayRequestMock.mock.calls[0]?.[0]).toBe(
      "/api/v1/sub-fields/field/field-1",
    );
    expect(gatewayRequestMock.mock.calls[0]?.[1]?.method).toBe("POST");
    expect(bodyAt(0)).toEqual(subFieldInput);
    expect(gatewayRequestMock.mock.calls[1]?.[0]).toBe("/api/v1/sub-fields/sub-1");
    expect(gatewayRequestMock.mock.calls[1]?.[1]?.method).toBe("PUT");
    expect(bodyAt(1)).toEqual(subFieldInput);
    expect(gatewayRequestMock.mock.calls[2]?.[0]).toBe("/api/v1/sub-fields/sub-1");
    expect(gatewayRequestMock.mock.calls[2]?.[1]?.method).toBe("DELETE");
  });

  it("sends closure create, update, and delete requests", async () => {
    gatewayRequestMock.mockResolvedValue({ id: "closure-1" });
    const { createClosures, deleteClosure, updateClosure } = await import("./fields");
    const input = {
      subFieldIds: ["sub-1", "sub-2"],
      startDate: "2026-09-01",
      endDate: "2026-09-03",
      reason: "Maintenance",
    };

    await createClosures(input.subFieldIds, input.startDate, input.endDate, input.reason);
    await updateClosure("closure-1", { ...input, subFieldIds: ["sub-1"] });
    await deleteClosure("closure-1");

    expect(gatewayRequestMock.mock.calls[0]?.[0]).toBe("/api/v1/sub-fields/closures");
    expect(gatewayRequestMock.mock.calls[0]?.[1]?.method).toBe("POST");
    expect(bodyAt(0)).toEqual(input);
    expect(gatewayRequestMock.mock.calls[1]?.[0]).toBe(
      "/api/v1/sub-fields/closures/closure-1",
    );
    expect(gatewayRequestMock.mock.calls[1]?.[1]?.method).toBe("PUT");
    expect(bodyAt(1)).toEqual({ ...input, subFieldIds: ["sub-1"] });
    expect(gatewayRequestMock.mock.calls[2]?.[0]).toBe(
      "/api/v1/sub-fields/closures/closure-1",
    );
    expect(gatewayRequestMock.mock.calls[2]?.[1]?.method).toBe("DELETE");
  });

  it("sends image upload slot, confirmation, delete, and order requests", async () => {
    gatewayRequestMock.mockResolvedValue([{ id: 10 }]);
    const {
      changeFieldImageOrder,
      confirmFieldImageUploads,
      deleteFieldImage,
      requestFieldImageUploadSlots,
    } = await import("./fields");
    const result: CloudinaryUploadResult = {
      public_id: "fields/field-1/image-1",
      secure_url: "https://cdn.example.com/image-1.webp",
      version: 123,
      signature: "cloudinary-signature",
      format: "webp",
      width: 1200,
      height: 800,
      bytes: 456789,
    };

    await requestFieldImageUploadSlots("field-1", "request-1", 2);
    await confirmFieldImageUploads("field-1", [result]);
    await deleteFieldImage("field-1", 10);
    await changeFieldImageOrder("field-1", [12, 10, 11]);

    expect(gatewayRequestMock.mock.calls[0]?.[0]).toBe(
      "/api/v1/fields/field-1/images/upload-slots",
    );
    expect(gatewayRequestMock.mock.calls[0]?.[1]?.method).toBe("POST");
    expect(bodyAt(0)).toEqual({ requestId: "request-1", count: 2 });
    expect(gatewayRequestMock.mock.calls[1]?.[0]).toBe(
      "/api/v1/fields/field-1/images/confirm",
    );
    expect(gatewayRequestMock.mock.calls[1]?.[1]?.method).toBe("POST");
    expect(bodyAt(1)).toEqual({
      uploads: [
        {
          publicId: result.public_id,
          secureUrl: result.secure_url,
          version: result.version,
          signature: result.signature,
          format: result.format,
          width: result.width,
          height: result.height,
          bytes: result.bytes,
        },
      ],
    });
    expect(gatewayRequestMock.mock.calls[2]?.[0]).toBe(
      "/api/v1/fields/field-1/images/10",
    );
    expect(gatewayRequestMock.mock.calls[2]?.[1]?.method).toBe("DELETE");
    expect(gatewayRequestMock.mock.calls[3]?.[0]).toBe(
      "/api/v1/fields/field-1/images/order",
    );
    expect(gatewayRequestMock.mock.calls[3]?.[1]?.method).toBe("PUT");
    expect(bodyAt(3)).toEqual({ imageIds: [12, 10, 11] });
  });
});
