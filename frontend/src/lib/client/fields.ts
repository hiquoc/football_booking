import type {
  Field,
  FieldCardData,
  FieldCardFilters,
  FieldDetails,
  FieldStatus,
  FavoriteCheckResponse,
  OperatingHours,
  PageResponse,
  Review,
  SubField,
  SubFieldFilterOption,
} from "@/lib/api/types";
import { requestJson } from "./http";
import { jsonBody } from "./http";

export function fetchFields(page: number, size = 9, status?: FieldStatus) {
  const query = new URLSearchParams({ page: String(page), size: String(size) });
  if (status) query.set("status", status);
  return requestJson<PageResponse<Field>>(`/api/fields?${query}`);
}

export function fetchFieldCards(
  page: number,
  size = 9,
  filters: FieldCardFilters = {},
) {
  const query = new URLSearchParams({ page: String(page), size: String(size) });
  Object.entries(filters).forEach(([key, value]) => {
    if (value) query.set(key, value);
  });
  return requestJson<PageResponse<FieldCardData>>(
    `/api/field-cards?${query}`,
  );
}

export function fetchFieldDetails(id: string) {
  return requestJson<FieldDetails>(
    `/api/fields/${encodeURIComponent(id)}/details`,
  );
}

export function fetchField(id: string) {
  return requestJson<Field>(`/api/fields/${encodeURIComponent(id)}`);
}

export function fetchFieldOperatingHours(id: string) {
  return requestJson<OperatingHours[]>(
    `/api/fields/${encodeURIComponent(id)}/operating-hours`,
  );
}

export function fetchSubFields(id: string) {
  return requestJson<SubField[]>(
    `/api/fields/${encodeURIComponent(id)}/sub-fields`,
  );
}

export function fetchSubFieldFilterOptions(search = "") {
  const query = new URLSearchParams();
  if (search.trim()) query.set("search", search.trim());
  return requestJson<SubFieldFilterOption[]>(
    `/api/subfields/filter-options${query.size ? `?${query}` : ""}`,
  );
}

export function fetchFieldReviews(id: string, page = 0, size = 6) {
  const query = new URLSearchParams({ page: String(page), size: String(size) });
  return requestJson<PageResponse<Review>>(
    `/api/fields/${encodeURIComponent(id)}/reviews?${query}`,
  );
}

export function submitFieldReview(
  fieldId: string,
  rating: number,
  comment?: string,
) {
  return requestJson<Review>(
    `/api/fields/${encodeURIComponent(fieldId)}/reviews`,
    { method: "POST", ...jsonBody({ rating, comment }) },
  );
}

export function submitFieldStatus(id: string, status: FieldStatus) {
  return requestJson<Field>(`/api/admin/fields/${encodeURIComponent(id)}/status`, {
    method: "PATCH",
    ...jsonBody({ status }),
  });
}

export function fetchFavoriteFields(page: number, size = 4) {
  const query = new URLSearchParams({ page: String(page), size: String(size) });
  return requestJson<PageResponse<Field>>(`/api/users/me/favorites?${query}`);
}

export function addFavoriteField(fieldId: string) {
  return requestJson<Field>(
    `/api/users/me/favorites/${encodeURIComponent(fieldId)}`,
    { method: "POST" },
  );
}

export function removeFavoriteField(fieldId: string) {
  return requestJson<void>(
    `/api/users/me/favorites/${encodeURIComponent(fieldId)}`,
    { method: "DELETE" },
  );
}

export function checkFavoriteField(fieldId: string) {
  return requestJson<FavoriteCheckResponse>(
    `/api/users/me/favorites/check/${encodeURIComponent(fieldId)}`,
  );
}
