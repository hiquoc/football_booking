import type {
  CommunityApplication,
  AdminModerationInput,
  CommunityPost,
  CommunityPostFilters,
  CommunityReport,
  CommunityReportReason,
  CommunityReportStatus,
  CreateCommunityPostInput,
  PageResponse,
  UpdateCommunityPostInput,
} from "@/lib/api/types";
import { jsonBody, requestJson } from "./http";

function query(page: number, size: number, filters: CommunityPostFilters) {
  const params = new URLSearchParams({ page: String(page), size: String(size) });
  Object.entries(filters).forEach(([key, value]) => {
    if (value) params.set(key, String(value));
  });
  return params;
}

export function fetchCommunityPosts(page: number, size = 10, filters: CommunityPostFilters = {}) {
  return requestJson<PageResponse<CommunityPost>>(`/api/community-posts?${query(page, size, filters)}`);
}

export function fetchCommunityPost(id: string) {
  return requestJson<CommunityPost>(`/api/community-posts/${encodeURIComponent(id)}`);
}

export function submitCommunityPost(input: CreateCommunityPostInput) {
  return requestJson<CommunityPost>("/api/community-posts", { method: "POST", ...jsonBody(input) });
}

export function submitCommunityPostUpdate(id: string, input: UpdateCommunityPostInput) {
  return requestJson<CommunityPost>(`/api/community-posts/${encodeURIComponent(id)}`, { method: "PUT", ...jsonBody(input) });
}

export function submitCommunityPostAction(id: string, action: "close" | "full") {
  return requestJson<CommunityPost>(`/api/community-posts/${encodeURIComponent(id)}/${action}`, { method: "PATCH" });
}

export function submitCommunityApplication(id: string, body: Record<string, unknown>) {
  return requestJson<CommunityApplication>(`/api/community-posts/${encodeURIComponent(id)}/applications`, {
    method: "POST",
    ...jsonBody(body),
  });
}

export function submitCommunityWithdraw(id: string) {
  return requestJson<CommunityApplication>(`/api/community-posts/${encodeURIComponent(id)}/applications/withdraw`, { method: "PATCH" });
}

export function submitCommunityDecision(id: string, applicationId: string, decision: "accept" | "reject") {
  return requestJson<CommunityApplication>(
    `/api/community-posts/${encodeURIComponent(id)}/applications/${encodeURIComponent(applicationId)}/${decision}`,
    { method: "PATCH" },
  );
}

export function submitCommunityReport(id: string, reason: CommunityReportReason, description?: string) {
  return requestJson<CommunityReport>(`/api/community-posts/${encodeURIComponent(id)}/reports`, {
    method: "POST",
    ...jsonBody({ reason, description }),
  });
}

export function submitOwnerHideCommunityPost(id: string, reason: string) {
  return requestJson<CommunityPost>(`/api/community-posts/${encodeURIComponent(id)}/owner-hide`, {
    method: "PATCH",
    ...jsonBody({ reason }),
  });
}

export function fetchCommunityReports(page: number, size = 20, status?: CommunityReportStatus) {
  const query = new URLSearchParams({ page: String(page), size: String(size) });
  if (status) query.set("status", status);
  return requestJson<PageResponse<CommunityReport>>(`/api/admin/community-moderation/reports?${query}`);
}

export function submitAdminModeration(input: AdminModerationInput) {
  return requestJson("/api/admin/community-moderation/reviews", {
    method: "POST",
    ...jsonBody(input),
  });
}
