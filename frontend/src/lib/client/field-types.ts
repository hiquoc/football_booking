import type { FieldType, FieldTypeInput } from "@/lib/api/types";
import { jsonBody, requestJson } from "./http";

export function fetchFieldTypes() {
  return requestJson<FieldType[]>("/api/field-types");
}

export function submitFieldType(input: FieldTypeInput) {
  return requestJson<FieldType>("/api/field-types", {
    method: "POST",
    ...jsonBody(input),
  });
}

export function submitFieldTypeUpdate(id: number, input: FieldTypeInput) {
  return requestJson<FieldType>(`/api/field-types/${id}`, {
    method: "PUT",
    ...jsonBody(input),
  });
}

export function submitFieldTypeDelete(id: number) {
  return requestJson<void>(`/api/field-types/${id}`, { method: "DELETE" });
}
