import "server-only";

import { revalidateTag } from "next/cache";
import { unstable_cache } from "next/cache";
import type {
  Booking,
  Field,
  FieldCardData,
  FieldCardFilters,
  FieldDetails,
  FieldStatus,
  FieldClosure,
  FieldImage,
  FieldSearchOption,
  FieldInput,
  FieldEmployee,
  FavoriteCheckResponse,
  ImageUploadSlot,
  CloudinaryUploadResult,
  OperatingHours,
  PageResponse,
  Review,
  SubField,
  SubFieldFilterOption,
  SubFieldInput,
} from "@/lib/api/types";
import {
  authenticatedGatewayRequest,
  sessionGatewayRequest,
} from "./authenticated-gateway";
import { gatewayRequest } from "./gateway";
import { getAccessToken } from "./session";

const fieldCacheTag = (id: string) => `field-${id}`;

export async function getFeaturedFields() {
  return sessionGatewayRequest<PageResponse<FieldCardData>>(
    "/api/v1/fields/cards?page=0&size=6&sortBy=rating&direction=desc",
    { next: { revalidate: 60 } },
  );
}

export async function getRecentlyBookedFieldCards(userId: string, limit = 4) {
  const accessToken = await getAccessToken();
  if (!accessToken) return [];

  return unstable_cache(
    async () => {
      const bookings = await gatewayRequest<PageResponse<Booking>>(
        "/api/v1/bookings/my?page=0&size=12&sort=createdAt,desc",
        { headers: { Authorization: `Bearer ${accessToken}` } },
      );
      const fieldIds = Array.from(
        new Set(
          bookings.content
            .map((booking) => booking.fieldId)
            .filter((fieldId): fieldId is string => Boolean(fieldId)),
        ),
      ).slice(0, limit);

      const fields = await Promise.all(
        fieldIds.map((fieldId) =>
          gatewayRequest<Field>(`/api/v1/fields/${encodeURIComponent(fieldId)}`, {
            headers: { Authorization: `Bearer ${accessToken}` },
          }).catch(() => null),
        ),
      );

      return fields
        .filter((field): field is Field => Boolean(field))
        .map(fieldToCardData);
    },
    [`recently-booked-fields-${userId}`],
    { revalidate: 120, tags: [`recently-booked-fields-${userId}`] },
  )();
}

function fieldToCardData(field: Field): FieldCardData {
  return {
    id: field.id,
    name: field.name,
    address: field.address,
    ward: field.ward,
    province: field.province,
    latitude: field.latitude,
    longitude: field.longitude,
    ratingAverage: field.ratingAverage,
    totalReviews: field.totalReviews,
    primaryImageUrl:
      field.images.find((image) => image.isPrimary)?.imageUrl ??
      field.images[0]?.imageUrl ??
      null,
    fieldTypes: field.fieldTypes.map((type) => type.name),
    distanceKm: null,
    isSaved: field.isSaved ?? field.isFavorite,
    isFavorite: field.isFavorite ?? field.isSaved,
  };
}

export async function getFieldCards(
  page = 0,
  size = 9,
  filters: FieldCardFilters = {},
) {
  const query = new URLSearchParams({ page: String(page), size: String(size) });
  Object.entries(filters).forEach(([key, value]) => {
    if (value) query.set(key, value);
  });
  if (!query.has("sortBy")) query.set("sortBy", "rating");
  if (!query.has("direction")) query.set("direction", "desc");
  return sessionGatewayRequest<PageResponse<FieldCardData>>(
    `/api/v1/fields/cards?${query}`,
  );
}

export async function getFields(page = 0, size = 9, status?: FieldStatus) {
  const query = new URLSearchParams({
    page: String(page),
    size: String(size),
    sort: "ratingAverage,desc",
  });
  if (status) {
    query.set("status", status);
    return authenticatedGatewayRequest<PageResponse<Field>>(
      `/api/v1/fields?${query}`,
    );
  }
  return sessionGatewayRequest<PageResponse<Field>>(`/api/v1/fields?${query}`);
}

export async function searchAdminFields(keyword = "") {
  const query = new URLSearchParams();
  if (keyword.trim()) query.set("keyword", keyword.trim());
  return authenticatedGatewayRequest<FieldSearchOption[]>(
    `/api/v1/fields/admin/search${query.size ? `?${query}` : ""}`,
  );
}

export async function getOwnerFields(page = 0, size = 10) {
  const query = new URLSearchParams({
    page: String(page),
    size: String(size),
    sort: "createdAt,desc",
  });
  return authenticatedGatewayRequest<PageResponse<Field>>(
    `/api/v1/fields/owner?${query}`,
  );
}

export async function getAssignedFields(page = 0, size = 10) {
  const query = new URLSearchParams({
    page: String(page),
    size: String(size),
    sort: "createdAt,desc",
  });
  return authenticatedGatewayRequest<PageResponse<Field>>(
    `/api/v1/fields/employee/assigned?${query}`,
  );
}

export function getFieldEmployees(fieldId: string) {
  return authenticatedGatewayRequest<FieldEmployee[]>(
    `/api/v1/fields/${encodeURIComponent(fieldId)}/employees`,
  );
}

export function assignFieldEmployee(fieldId: string, employeeId: string) {
  return authenticatedGatewayRequest<FieldEmployee>(
    `/api/v1/fields/${encodeURIComponent(fieldId)}/employees`,
    { method: "POST", body: JSON.stringify({ employeeId }) },
  );
}

export function removeFieldEmployee(fieldId: string, employeeId: string) {
  return authenticatedGatewayRequest<null>(
    `/api/v1/fields/${encodeURIComponent(fieldId)}/employees/${encodeURIComponent(employeeId)}`,
    { method: "DELETE" },
  );
}

export async function getField(id: string) {
  return sessionGatewayRequest<Field>(`/api/v1/fields/${encodeURIComponent(id)}`, {
    next: { revalidate: 60, tags: [fieldCacheTag(id)] },
  });
}

export async function getFieldDetails(id: string) {
  return sessionGatewayRequest<FieldDetails>(
    `/api/v1/fields/${encodeURIComponent(id)}/details`,
    { next: { revalidate: 60, tags: [fieldCacheTag(id)] } },
  );
}

export async function getFieldOperatingHours(id: string) {
  return gatewayRequest<OperatingHours[]>(
    `/api/v1/fields/${encodeURIComponent(id)}/operating-hours`,
    { next: { revalidate: 60 } },
  );
}

export async function getSubFields(id: string) {
  return gatewayRequest<SubField[]>(
    `/api/v1/sub-fields/field/${encodeURIComponent(id)}`,
    {
      next: { revalidate: 60 },
    },
  );
}

export function getSubFieldFilterOptions(search?: string) {
  const query = new URLSearchParams();
  if (search?.trim()) query.set("search", search.trim());
  return authenticatedGatewayRequest<SubFieldFilterOption[]>(
    `/api/v1/subfields/filter-options${query.size ? `?${query}` : ""}`,
  );
}

export async function getFieldReviews(id: string, page = 0, size = 6) {
  const query = new URLSearchParams({ page: String(page), size: String(size) });
  return gatewayRequest<PageResponse<Review>>(
    `/api/v1/reviews/field/${encodeURIComponent(id)}?${query}`,
    {
      next: { revalidate: 60 },
    },
  );
}

export async function createFieldReview(
  fieldId: string,
  rating: number,
  comment?: string,
) {
  const review = await authenticatedGatewayRequest<Review>("/api/v1/reviews", {
    method: "POST",
    body: JSON.stringify({ fieldId, rating, comment }),
  });
  revalidateTag(fieldCacheTag(fieldId), "max");
  return review;
}

export function createField(input: FieldInput) {
  return authenticatedGatewayRequest<Field>("/api/v1/fields", {
    method: "POST",
    body: JSON.stringify(input),
  });
}

export function updateFieldStatus(id: string, status: FieldStatus) {
  return authenticatedGatewayRequest<Field>(
    `/api/v1/fields/${encodeURIComponent(id)}/status`,
    { method: "PATCH", body: JSON.stringify({ status }) },
  );
}

export async function updateField(id: string, input: FieldInput) {
  const field = await authenticatedGatewayRequest<Field>(
    `/api/v1/fields/${encodeURIComponent(id)}`,
    { method: "PUT", body: JSON.stringify(input) },
  );
  revalidateTag(fieldCacheTag(id), "max");
  return field;
}

export async function createSubField(fieldId: string, input: SubFieldInput) {
  const subField = await authenticatedGatewayRequest<SubField>(
    `/api/v1/sub-fields/field/${encodeURIComponent(fieldId)}`,
    { method: "POST", body: JSON.stringify(input) },
  );
  revalidateTag(fieldCacheTag(fieldId), "max");
  return subField;
}

export async function deleteSubField(fieldId: string, id: string) {
  const result = await authenticatedGatewayRequest<null>(
    `/api/v1/sub-fields/${encodeURIComponent(id)}`,
    { method: "DELETE" },
  );
  revalidateTag(fieldCacheTag(fieldId), "max");
  return result;
}

export async function updateSubField(fieldId: string, id: string, input: SubFieldInput) {
  const subField = await authenticatedGatewayRequest<SubField>(
    `/api/v1/sub-fields/${encodeURIComponent(id)}`,
    { method: "PUT", body: JSON.stringify(input) },
  );
  revalidateTag(fieldCacheTag(fieldId), "max");
  return subField;
}

export function getSubFieldClosures(id: string) {
  return authenticatedGatewayRequest<FieldClosure[]>(
    `/api/v1/sub-fields/${encodeURIComponent(id)}/closures`,
  );
}

export function createClosures(
  subFieldIds: string[],
  startDate: string,
  endDate: string,
  reason: string,
) {
  return authenticatedGatewayRequest<FieldClosure[]>(
    "/api/v1/sub-fields/closures",
    {
      method: "POST",
      body: JSON.stringify({ subFieldIds, startDate, endDate, reason }),
    },
  );
}

export function deleteClosure(id: string) {
  return authenticatedGatewayRequest<null>(
    `/api/v1/sub-fields/closures/${encodeURIComponent(id)}`,
    { method: "DELETE" },
  );
}

export function updateClosure(
  id: string,
  input: {
    subFieldIds: string[];
    startDate: string;
    endDate: string;
    reason: string;
  },
) {
  return authenticatedGatewayRequest<FieldClosure>(
    `/api/v1/sub-fields/closures/${encodeURIComponent(id)}`,
    { method: "PUT", body: JSON.stringify(input) },
  );
}

export function requestFieldImageUploadSlots(fieldId: string, requestId: string, count: number) {
  return authenticatedGatewayRequest<ImageUploadSlot[]>(
    `/api/v1/fields/${encodeURIComponent(fieldId)}/images/upload-slots`,
    { method: "POST", body: JSON.stringify({ requestId, count }) },
  );
}

export async function confirmFieldImageUploads(fieldId: string, results: CloudinaryUploadResult[]) {
  const images = await authenticatedGatewayRequest<FieldImage[]>(
    `/api/v1/fields/${encodeURIComponent(fieldId)}/images/confirm`,
    {
      method: "POST",
        body: JSON.stringify({ uploads: results.map((result) => ({
          publicId: result.public_id,
        secureUrl: result.secure_url,
        version: result.version,
        signature: result.signature,
        format: result.format,
        width: result.width,
        height: result.height,
          bytes: result.bytes,
        })) }),
    },
  );
  revalidateTag(fieldCacheTag(fieldId), "max");
  return images;
}

export async function deleteFieldImage(fieldId: string, imageId: number) {
  const result = await authenticatedGatewayRequest<null>(
    `/api/v1/fields/${encodeURIComponent(fieldId)}/images/${imageId}`,
    { method: "DELETE" },
  );
  revalidateTag(fieldCacheTag(fieldId), "max");
  return result;
}

export async function changeFieldImageOrder(
  fieldId: string,
  imageIds: number[],
) {
  const images = await authenticatedGatewayRequest<FieldImage[]>(
    `/api/v1/fields/${encodeURIComponent(fieldId)}/images/order`,
    { method: "PUT", body: JSON.stringify({ imageIds }) },
  );
  revalidateTag(fieldCacheTag(fieldId), "max");
  return images;
}

export function getFavoriteFields(page = 0, size = 4) {
  const query = new URLSearchParams({
    page: String(page),
    size: String(size),
    sort: "createdAt,desc",
  });
  return authenticatedGatewayRequest<PageResponse<Field>>(
    `/api/v1/users/me/favorites?${query}`,
  );
}

export async function addFavoriteField(fieldId: string) {
  const field = await authenticatedGatewayRequest<Field>(
    `/api/v1/users/me/favorites/${encodeURIComponent(fieldId)}`,
    { method: "POST" },
  );
  revalidateTag(fieldCacheTag(fieldId), "max");
  return field;
}

export async function removeFavoriteField(fieldId: string) {
  const result = await authenticatedGatewayRequest<null>(
    `/api/v1/users/me/favorites/${encodeURIComponent(fieldId)}`,
    { method: "DELETE" },
  );
  revalidateTag(fieldCacheTag(fieldId), "max");
  return result;
}

export function checkFavoriteField(fieldId: string) {
  return authenticatedGatewayRequest<FavoriteCheckResponse>(
    `/api/v1/users/me/favorites/check/${encodeURIComponent(fieldId)}`,
  );
}
