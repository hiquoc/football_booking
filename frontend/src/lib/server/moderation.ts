import "server-only";

import type {
  CreatePaymentDisputeInput,
  FieldViolation,
  PageResponse,
  PaymentDisputeReport,
  PaymentDisputeStatus,
} from "@/lib/api/types";
import { authenticatedGatewayRequest } from "./authenticated-gateway";
import { getPublicProfilesById } from "./users";

export function reportNoShow(bookingId: string) {
  return authenticatedGatewayRequest<FieldViolation>("/api/v1/moderation/owner/no-shows", {
    method: "POST",
    body: JSON.stringify({ bookingId }),
  });
}

export async function getFieldViolations(fieldId: string, page = 0, size = 20) {
  return withViolationUserNames(await authenticatedGatewayRequest<PageResponse<FieldViolation>>(
    `/api/v1/moderation/owner/fields/${encodeURIComponent(fieldId)}/violations?page=${page}&size=${size}`,
  ));
}

export function getUserFieldViolations(userId: string, page = 0, size = 5) {
  const query = new URLSearchParams({ page: String(page), size: String(size) });
  return authenticatedGatewayRequest<PageResponse<FieldViolation>>(
    `/api/v1/moderation/admin/users/${encodeURIComponent(userId)}/field-violations?${query}`,
  );
}

export async function getBannedClients(fieldId: string, page = 0, size = 20) {
  return withViolationUserNames(await authenticatedGatewayRequest<PageResponse<FieldViolation>>(
    `/api/v1/moderation/owner/fields/${encodeURIComponent(fieldId)}/banned-clients?page=${page}&size=${size}`,
  ));
}

export function banClient(fieldId: string, userId: string) {
  return authenticatedGatewayRequest<FieldViolation>(
    `/api/v1/moderation/owner/fields/${encodeURIComponent(fieldId)}/banned-clients/${encodeURIComponent(userId)}/ban`,
    { method: "PATCH" },
  );
}

export function unbanClient(fieldId: string, userId: string) {
  return authenticatedGatewayRequest<FieldViolation>(
    `/api/v1/moderation/owner/fields/${encodeURIComponent(fieldId)}/banned-clients/${encodeURIComponent(userId)}/unban`,
    { method: "PATCH" },
  );
}

async function withViolationUserNames(page: PageResponse<FieldViolation>) {
  const profiles = await getPublicProfilesById(page.content.map((item) => item.userId));
  return {
    ...page,
    content: page.content.map((item) => ({
      ...item,
      userDisplayName: profiles.get(item.userId)?.personal.fullName?.trim() || item.userId,
    })),
  };
}

export function createPaymentDispute(input: CreatePaymentDisputeInput) {
  return authenticatedGatewayRequest<PaymentDisputeReport>("/api/v1/moderation/owner/payment-disputes", {
    method: "POST",
    body: JSON.stringify(input),
  });
}

export function getOwnerPaymentDisputes(page = 0, size = 20) {
  return authenticatedGatewayRequest<PageResponse<PaymentDisputeReport>>(
    `/api/v1/moderation/owner/payment-disputes?page=${page}&size=${size}`,
  );
}

export function getAdminPaymentDisputes(page = 0, size = 20, status?: PaymentDisputeStatus) {
  const query = new URLSearchParams({ page: String(page), size: String(size) });
  if (status) query.set("status", status);
  return authenticatedGatewayRequest<PageResponse<PaymentDisputeReport>>(
    `/api/v1/moderation/admin/payment-disputes?${query}`,
  );
}

export function reviewPaymentDispute(reportId: string, approved: boolean, adminNote: string) {
  return authenticatedGatewayRequest<PaymentDisputeReport>(
    `/api/v1/moderation/admin/payment-disputes/${encodeURIComponent(reportId)}/review`,
    {
      method: "PATCH",
      body: JSON.stringify({ approved, adminNote }),
    },
  );
}
