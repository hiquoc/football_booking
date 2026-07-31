import "server-only";

import type {
  PageResponse,
  PublicProfile,
  UpdateProfileInput,
  User,
  AvatarUploadSlot,
  CloudinaryUploadResult,
} from "@/lib/api/types";
import { authenticatedGatewayRequest } from "./authenticated-gateway";

export function getMyProfile() {
  return authenticatedGatewayRequest<User>("/api/v1/users/me");
}

export function getMyPublicProfile() {
  return authenticatedGatewayRequest<PublicProfile>("/api/v1/users/me/profile");
}

export function getPublicProfile(id: string) {
  return authenticatedGatewayRequest<PublicProfile>(
    `/api/v1/users/${encodeURIComponent(id)}/profile`,
  );
}

export async function getPublicProfilesById(ids: string[]) {
  const uniqueIds = Array.from(new Set(ids.filter(Boolean)));
  const profiles = await Promise.all(
    uniqueIds.map(async (id) => {
      try {
        return [id, await getPublicProfile(id)] as const;
      } catch {
        return [id, null] as const;
      }
    }),
  );
  return new Map(profiles);
}

export function getUsers(page = 0, size = 10) {
  const query = new URLSearchParams({ page: String(page), size: String(size) });
  return authenticatedGatewayRequest<PageResponse<User>>(
    `/api/v1/users?${query}`,
  );
}

export function searchUsers(page = 0, size = 10, phoneNumber = "") {
  const query = new URLSearchParams({ page: String(page), size: String(size) });
  const trimmedPhone = phoneNumber.trim();
  if (trimmedPhone) query.set("phoneNumber", trimmedPhone);
  return authenticatedGatewayRequest<PageResponse<User>>(
    `/api/v1/users?${query}`,
  );
}

export function findEmployeeByPhone(phoneNumber: string) {
  const params = new URLSearchParams({ phoneNumber });
  return authenticatedGatewayRequest<User>(
    `/api/v1/users/employees/by-phone?${params}`,
  );
}

export function updateMyProfile(input: UpdateProfileInput) {
  return authenticatedGatewayRequest<PublicProfile>("/api/v1/users/me/profile", {
    method: "PATCH",
    body: JSON.stringify(input),
  });
}

export function requestAvatarUploadSlot(requestId: string) {
  return authenticatedGatewayRequest<AvatarUploadSlot>("/api/v1/users/me/avatar/upload-slot", {
    method: "POST", body: JSON.stringify({ requestId }),
  });
}

export function requestTeamPhotoUploadSlot(requestId: string) {
  return authenticatedGatewayRequest<AvatarUploadSlot>("/api/v1/users/me/team-photo/upload-slot", {
    method: "POST", body: JSON.stringify({ requestId }),
  });
}

export function confirmAvatarUpload(result: CloudinaryUploadResult) {
  return authenticatedGatewayRequest<User>("/api/v1/users/me/avatar/confirm", {
    method: "POST", body: JSON.stringify({ publicId: result.public_id, secureUrl: result.secure_url,
      version: result.version, signature: result.signature, format: result.format,
      width: result.width, height: result.height, bytes: result.bytes }),
  });
}

export function confirmTeamPhotoUpload(result: CloudinaryUploadResult) {
  return authenticatedGatewayRequest<User>("/api/v1/users/me/team-photo/confirm", {
    method: "POST", body: JSON.stringify({ publicId: result.public_id, secureUrl: result.secure_url,
      version: result.version, signature: result.signature, format: result.format,
      width: result.width, height: result.height, bytes: result.bytes }),
  });
}

export function updateUserRole(id: string, userType: User["userType"]) {
  return authenticatedGatewayRequest<User>(
    `/api/v1/users/${encodeURIComponent(id)}/role`,
    { method: "PUT", body: JSON.stringify({ userType }) },
  );
}

export function updateUserStatus(id: string, status: "ACTIVE" | "PLATFORM_BANNED") {
  return authenticatedGatewayRequest<User>(
    `/api/v1/users/${encodeURIComponent(id)}/status`,
    { method: "PATCH", body: JSON.stringify({ status }) },
  );
}
