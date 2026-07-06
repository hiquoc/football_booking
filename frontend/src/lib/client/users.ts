import type {
  PageResponse,
  UpdateProfileInput,
  User,
  AvatarUploadSlot,
  CloudinaryUploadResult,
} from "@/lib/api/types";
import { jsonBody, requestJson } from "./http";

export function fetchMyProfile() {
  return requestJson<User>("/api/profile");
}

export function fetchUsers(page: number, size = 10) {
  const query = new URLSearchParams({ page: String(page), size: String(size) });
  return requestJson<PageResponse<User>>(`/api/admin/users?${query}`);
}

export function submitProfileUpdate(input: UpdateProfileInput) {
  return requestJson<User>("/api/profile", {
    method: "PATCH",
    ...jsonBody(input),
  });
}

export async function submitAvatar(file: File) {
  if (!["image/jpeg", "image/png", "image/webp"].includes(file.type)) throw new Error("Chỉ hỗ trợ ảnh JPEG, PNG và WEBP");
  const slot = await requestJson<AvatarUploadSlot>("/api/profile/avatar/upload-slot", {
    method: "POST", ...jsonBody({ requestId: crypto.randomUUID() }),
  });
  const data = new FormData();
  data.set("file", file); data.set("api_key", slot.apiKey); data.set("timestamp", String(slot.timestamp));
  data.set("public_id", slot.publicId); data.set("signature", slot.signature); data.set("overwrite", "false");
  const response = await fetch(slot.uploadUrl, { method: "POST", body: data });
  if (!response.ok) throw new Error("Cloudinary từ chối tải ảnh lên");
  return requestJson<User>("/api/profile/avatar/confirm", {
    method: "POST", ...jsonBody((await response.json()) as CloudinaryUploadResult),
  });
}

export function submitUserRole(id: string, userType: User["userType"]) {
  return requestJson<User>(`/api/admin/users/${encodeURIComponent(id)}/role`, {
    method: "PUT",
    ...jsonBody({ userType }),
  });
}
