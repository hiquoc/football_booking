import type {
  Field,
  FieldClosure,
  FieldImage,
  FieldInput,
  PageResponse,
  SubField,
  SubFieldInput,
  ImageUploadSlot,
  CloudinaryUploadResult,
} from "@/lib/api/types";
import { jsonBody, requestJson } from "./http";
export function fetchOwnerFields(page: number, size = 10) {
  const query = new URLSearchParams({ page: String(page), size: String(size) });
  return requestJson<PageResponse<Field>>(`/api/owner/fields?${query}`);
}
export function submitField(input: FieldInput) {
  return requestJson<Field>("/api/owner/fields", {
    method: "POST",
    ...jsonBody(input),
  });
}
export function submitFieldUpdate(id: string, input: FieldInput) {
  return requestJson<Field>(`/api/owner/fields/${id}`, {
    method: "PUT",
    ...jsonBody(input),
  });
}
export function submitSubField(fieldId: string, input: SubFieldInput) {
  return requestJson<SubField>(`/api/owner/fields/${fieldId}/sub-fields`, {
    method: "POST",
    ...jsonBody(input),
  });
}
export function submitSubFieldDelete(fieldId: string, id: string) {
  return requestJson<void>(`/api/owner/fields/${fieldId}/sub-fields/${id}`, {
    method: "DELETE",
  });
}
export function submitSubFieldUpdate(
  fieldId: string,
  id: string,
  input: SubFieldInput,
) {
  return requestJson<SubField>(
    `/api/owner/fields/${fieldId}/sub-fields/${id}`,
    { method: "PUT", ...jsonBody(input) },
  );
}
export function fetchClosures(fieldId: string, subFieldId: string) {
  return requestJson<FieldClosure[]>(
    `/api/owner/fields/${fieldId}/closures?subFieldId=${subFieldId}`,
  );
}
export function submitClosure(
  fieldId: string,
  input: {
    subFieldIds: string[];
    startDate: string;
    endDate: string;
    reason: string;
  },
) {
  return requestJson<FieldClosure[]>(`/api/owner/fields/${fieldId}/closures`, {
    method: "POST",
    ...jsonBody(input),
  });
}
export function submitClosureDelete(fieldId: string, id: string) {
  return requestJson<void>(`/api/owner/fields/${fieldId}/closures/${id}`, {
    method: "DELETE",
  });
}
export function submitClosureUpdate(
  fieldId: string,
  id: string,
  input: {
    subFieldIds: string[];
    startDate: string;
    endDate: string;
    reason: string;
  },
) {
  return requestJson<FieldClosure>(
    `/api/owner/fields/${fieldId}/closures/${id}`,
    { method: "PUT", ...jsonBody(input) },
  );
}
function requestUploadSlots(fieldId: string, requestId: string, count: number) {
  return requestJson<ImageUploadSlot[]>(`/api/owner/fields/${fieldId}/images/upload-slots`, {
    method: "POST",
    ...jsonBody({ requestId, count }),
  });
}

function confirmUploads(fieldId: string, results: CloudinaryUploadResult[]) {
  return requestJson<FieldImage[]>(`/api/owner/fields/${fieldId}/images/confirm`, {
    method: "POST",
    ...jsonBody(results),
  });
}

async function uploadToCloudinary(file: File, slot: ImageUploadSlot) {
  const data = new FormData();
  data.set("file", file);
  data.set("api_key", slot.apiKey);
  data.set("timestamp", String(slot.timestamp));
  data.set("public_id", slot.publicId);
  data.set("signature", slot.signature);
  data.set("overwrite", "false");
  const response = await fetch(slot.uploadUrl, { method: "POST", body: data });
  if (!response.ok) throw new Error("Cloudinary từ chối tải ảnh lên");
  return (await response.json()) as CloudinaryUploadResult;
}

export async function submitImages(fieldId: string, files: FileList) {
  const selected = Array.from(files);
  if (!selected.length || selected.length > 10) throw new Error("Vui lòng chọn từ 1 đến 10 ảnh");
  if (selected.some((file) => !["image/jpeg", "image/png", "image/webp"].includes(file.type))) {
    throw new Error("Chỉ hỗ trợ ảnh JPEG, PNG và WEBP");
  }
  const requestId = crypto.randomUUID();
  let slots: ImageUploadSlot[];
  try {
    slots = await requestUploadSlots(fieldId, requestId, selected.length);
  } catch {
    // A lost response may still have committed the placeholders; retry with the same key.
    slots = await requestUploadSlots(fieldId, requestId, selected.length);
  }
  const results = await Promise.all(selected.map((file, index) => uploadToCloudinary(file, slots[index])));
  return confirmUploads(fieldId, results);
}
export function submitImageDelete(fieldId: string, id: number) {
  return requestJson<void>(`/api/owner/fields/${fieldId}/images/${id}`, {
    method: "DELETE",
  });
}
export function submitImageOrderChange(
  fieldId: string,
  imageIds: number[],
) {
  return requestJson<FieldImage[]>(
    `/api/owner/fields/${fieldId}/images/order`,
    { method: "PUT", ...jsonBody({ imageIds }) },
  );
}
