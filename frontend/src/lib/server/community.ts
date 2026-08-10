import "server-only";

import type {
  CommunityApplication,
  AdminModerationInput,
  CommunityPost,
  CommunityPostFilters,
  CommunityReport,
  CommunityReportReason,
  CommunityReportStatus,
  CommunityViolation,
  CreateCommunityPostInput,
  MatchEvaluation,
  MatchEvaluationInput,
  PageResponse,
  UpdateCommunityPostInput,
} from "@/lib/api/types";
import { authenticatedGatewayRequest, sessionGatewayRequest } from "./authenticated-gateway";
import { gatewayRequest } from "./gateway";

function communityQuery(page: number, size: number, filters: CommunityPostFilters) {
  const query = new URLSearchParams({ page: String(page), size: String(size) });
  Object.entries(filters).forEach(([key, value]) => {
    if (value && !(key === "status" && value === "all")) query.set(key, String(value));
  });
  return query;
}

export function getCommunityPosts(page = 0, size = 10, filters: CommunityPostFilters = {}) {
  return gatewayRequest<PageResponse<CommunityPost>>(
    `/api/v1/community-posts?${communityQuery(page, size, filters)}`,
    { cache: "no-store" },
  );
}

export function getCommunityPost(id: string) {
  return sessionGatewayRequest<CommunityPost>(
    `/api/v1/community-posts/${encodeURIComponent(id)}`,
  );
}

export function createCommunityPost(input: CreateCommunityPostInput) {
  return authenticatedGatewayRequest<CommunityPost>("/api/v1/community-posts", {
    method: "POST",
    body: JSON.stringify(input),
  });
}

export function updateCommunityPost(id: string, input: UpdateCommunityPostInput) {
  return authenticatedGatewayRequest<CommunityPost>(`/api/v1/community-posts/${encodeURIComponent(id)}`, {
    method: "PUT",
    body: JSON.stringify(input),
  });
}

export function communityPostAction(id: string, action: "close" | "full") {
  return authenticatedGatewayRequest<CommunityPost>(`/api/v1/community-posts/${encodeURIComponent(id)}/${action}`, {
    method: "PATCH",
  });
}

export function applyCommunityPost(id: string, body: Record<string, unknown>) {
  return authenticatedGatewayRequest<CommunityApplication>(
    `/api/v1/community-posts/${encodeURIComponent(id)}/applications`,
    { method: "POST", body: JSON.stringify(body) },
  );
}

export function withdrawCommunityApplication(id: string) {
  return authenticatedGatewayRequest<CommunityApplication>(
    `/api/v1/community-posts/${encodeURIComponent(id)}/applications/withdraw`,
    { method: "PATCH" },
  );
}

export function decideCommunityApplication(id: string, applicationId: string, decision: "accept" | "reject") {
  return authenticatedGatewayRequest<CommunityApplication>(
    `/api/v1/community-posts/${encodeURIComponent(id)}/applications/${encodeURIComponent(applicationId)}/${decision}`,
    { method: "PATCH" },
  );
}

export function reportCommunityPost(id: string, reason: CommunityReportReason, description?: string) {
  return authenticatedGatewayRequest<CommunityReport>(
    `/api/v1/community-posts/${encodeURIComponent(id)}/reports`,
    { method: "POST", body: JSON.stringify({ reason, description }) },
  );
}

export function ownerHideCommunityPost(id: string, reason: string) {
  return authenticatedGatewayRequest<CommunityPost>(
    `/api/v1/community-posts/${encodeURIComponent(id)}/owner-hide`,
    { method: "PATCH", body: JSON.stringify({ reason }) },
  );
}

export function submitMatchEvaluation(id: string, input: MatchEvaluationInput) {
  return authenticatedGatewayRequest<MatchEvaluation>(
    `/api/v1/community-posts/${encodeURIComponent(id)}/evaluations`,
    { method: "POST", body: JSON.stringify(input) },
  );
}

export function getMatchEvaluations(id: string) {
  return authenticatedGatewayRequest<MatchEvaluation[]>(
    `/api/v1/community-posts/${encodeURIComponent(id)}/evaluations`,
    { cache: "no-store" },
  );
}

export function getCommunityReports(page = 0, size = 20, status?: CommunityReportStatus) {
  const query = new URLSearchParams({ page: String(page), size: String(size) });
  if (status) query.set("status", status);
  return authenticatedGatewayRequest<PageResponse<CommunityReport>>(
    `/api/v1/admin/community-moderation/reports?${query}`,
  );
}

export function submitCommunityModeration(input: AdminModerationInput) {
  return authenticatedGatewayRequest("/api/v1/admin/community-moderation/reviews", {
    method: "POST",
    body: JSON.stringify(input),
  });
}

export function getUserCommunityViolations(userId: string, page = 0, size = 5) {
  const query = new URLSearchParams({ page: String(page), size: String(size) });
  return authenticatedGatewayRequest<PageResponse<CommunityViolation>>(
    `/api/v1/admin/community-moderation/users/${encodeURIComponent(userId)}/violations?${query}`,
  );
}
