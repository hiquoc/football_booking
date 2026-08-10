import "server-only";

import { revalidateTag } from "next/cache";
import type { FieldType, FieldTypeInput } from "@/lib/api/types";
import { authenticatedGatewayRequest } from "./authenticated-gateway";
import { gatewayRequest } from "./gateway";

const fieldTypesCacheTag = "field-types";

export function getFieldTypes() {
  return gatewayRequest<FieldType[]>("/api/v1/field-types", {
    next: { revalidate: 31_536_000, tags: [fieldTypesCacheTag] },
  });
}

export function getSubFieldTypes() {
  return gatewayRequest<string[]>("/api/v1/sub-fields/types", {
    next: { revalidate: 31_536_000, tags: [fieldTypesCacheTag] },
  });
}

export async function createFieldType(input: FieldTypeInput) {
  const fieldType = await authenticatedGatewayRequest<FieldType>("/api/v1/field-types", {
    method: "POST",
    body: JSON.stringify(input),
  });
  revalidateTag(fieldTypesCacheTag, "max");
  return fieldType;
}

export async function updateFieldType(id: number, input: FieldTypeInput) {
  const fieldType = await authenticatedGatewayRequest<FieldType>(`/api/v1/field-types/${id}`, {
    method: "PUT",
    body: JSON.stringify(input),
  });
  revalidateTag(fieldTypesCacheTag, "max");
  return fieldType;
}

export async function deleteFieldType(id: number) {
  const result = await authenticatedGatewayRequest<null>(`/api/v1/field-types/${id}`, {
    method: "DELETE",
  });
  revalidateTag(fieldTypesCacheTag, "max");
  return result;
}
