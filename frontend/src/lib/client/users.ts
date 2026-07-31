import type {
  AvatarUploadSlot,
  CloudinaryUploadResult,
  PageResponse,
  PublicProfile,
  UpdateProfileInput,
  User,
  UserViolationHistory,
} from "@/lib/api/types";
import { jsonBody, requestJson } from "./http";

export function fetchMyProfile() {
  return requestJson<PublicProfile>("/api/profile");
}

export function fetchCurrentUser() {
  return requestJson<User>("/api/users/me");
}

export function fetchPublicProfile(id: string) {
  return requestJson<PublicProfile>(`/api/users/${encodeURIComponent(id)}/profile`);
}

export function fetchUsers(page: number, size = 10, phoneNumber = "") {
  const query = new URLSearchParams({ page: String(page), size: String(size) });
  const trimmedPhone = phoneNumber.trim();
  if (trimmedPhone) query.set("phoneNumber", trimmedPhone);
  return requestJson<PageResponse<User>>(`/api/admin/users?${query}`);
}

export function fetchEmployeeByPhone(phoneNumber: string) {
  const params = new URLSearchParams({ phoneNumber });
  return requestJson<User>(`/api/owner/employees/by-phone?${params}`);
}

export function submitProfileUpdate(input: UpdateProfileInput) {
  return requestJson<PublicProfile>("/api/profile", {
    method: "PATCH",
    ...jsonBody(input),
  });
}

export function submitAvatar(file: File) {
  return submitProfileImage(file, "/api/profile/avatar/upload-slot", "/api/profile/avatar/confirm");
}

export function submitTeamPhoto(file: File) {
  return submitProfileImage(file, "/api/profile/team-photo/upload-slot", "/api/profile/team-photo/confirm");
}

async function submitProfileImage(file: File, slotUrl: string, confirmUrl: string) {
  if (!["image/jpeg", "image/png", "image/webp"].includes(file.type)) {
    throw new Error("Chỉ hỗ trợ ảnh JPEG, PNG và WEBP");
  }
  const slot = await requestJson<AvatarUploadSlot>(slotUrl, {
    method: "POST",
    ...jsonBody({ requestId: crypto.randomUUID() }),
  });
  const data = new FormData();
  data.set("file", file);
  data.set("api_key", slot.apiKey);
  data.set("timestamp", String(slot.timestamp));
  data.set("public_id", slot.publicId);
  data.set("signature", slot.signature);
  data.set("overwrite", "false");
  const response = await fetch(slot.uploadUrl, { method: "POST", body: data });
  if (!response.ok) throw new Error("Cloudinary từ chối tải ảnh lên");
  return requestJson<User>(confirmUrl, {
    method: "POST",
    ...jsonBody((await response.json()) as CloudinaryUploadResult),
  });
}

export function submitUserRole(id: string, userType: User["userType"]) {
  return requestJson<User>(`/api/admin/users/${encodeURIComponent(id)}/role`, {
    method: "PUT",
    ...jsonBody({ userType }),
  });
}

export function submitUserStatus(id: string, status: "ACTIVE" | "PLATFORM_BANNED") {
  return requestJson<User>(`/api/admin/users/${encodeURIComponent(id)}/status`, {
    method: "PATCH",
    ...jsonBody({ status }),
  });
}

export function fetchUserViolations(id: string) {
  return requestJson<UserViolationHistory>(
    `/api/admin/users/${encodeURIComponent(id)}/violations?size=5`,
  );
}
