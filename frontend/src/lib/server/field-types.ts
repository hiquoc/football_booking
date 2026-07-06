import "server-only";

import type { FieldType, FieldTypeInput } from "@/lib/api/types";
import { authenticatedGatewayRequest } from "./authenticated-gateway";
import { gatewayRequest } from "./gateway";

export function getFieldTypes() {
  return gatewayRequest<FieldType[]>("/api/v1/field-types", {
    next: { revalidate: 300 },
  });
}

export function createFieldType(input: FieldTypeInput) {
  return authenticatedGatewayRequest<FieldType>("/api/v1/field-types", {
    method: "POST",
    body: JSON.stringify(input),
  });
}

export function updateFieldType(id: number, input: FieldTypeInput) {
  return authenticatedGatewayRequest<FieldType>(`/api/v1/field-types/${id}`, {
    method: "PUT",
    body: JSON.stringify(input),
  });
}

export function deleteFieldType(id: number) {
  return authenticatedGatewayRequest<null>(`/api/v1/field-types/${id}`, {
    method: "DELETE",
  });
}
