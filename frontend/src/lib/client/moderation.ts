import type {
  CreatePaymentDisputeInput,
  FieldViolation,
  PageResponse,
  PaymentDisputeReport,
  PaymentDisputeStatus,
} from "@/lib/api/types";
import { jsonBody, requestJson } from "./http";

export const fetchFieldViolations = (fieldId: string, page = 0, size = 20) =>
  requestJson<PageResponse<FieldViolation>>(`/api/owner/client-violations?fieldId=${encodeURIComponent(fieldId)}&page=${page}&size=${size}`);

export const fetchBannedClients = (fieldId: string, page = 0, size = 20) =>
  requestJson<PageResponse<FieldViolation>>(`/api/owner/banned-clients?fieldId=${encodeURIComponent(fieldId)}&page=${page}&size=${size}`);

export const submitUnbanClient = (fieldId: string, userId: string) =>
  requestJson<FieldViolation>(`/api/owner/banned-clients/${encodeURIComponent(userId)}/unban`, {
    method: "PATCH",
    ...jsonBody({ fieldId }),
  });

export const submitPaymentDispute = (input: CreatePaymentDisputeInput) =>
  requestJson<PaymentDisputeReport>("/api/owner/payment-disputes", {
    method: "POST",
    ...jsonBody(input),
  });

export const fetchOwnerPaymentDisputes = (page = 0, size = 20) =>
  requestJson<PageResponse<PaymentDisputeReport>>(`/api/owner/payment-disputes?page=${page}&size=${size}`);

export const fetchAdminPaymentDisputes = (page = 0, size = 20, status?: PaymentDisputeStatus) =>
  requestJson<PageResponse<PaymentDisputeReport>>(`/api/admin/payment-disputes?page=${page}&size=${size}${status ? `&status=${status}` : ""}`);

export const submitPaymentDisputeReview = (reportId: string, approved: boolean, adminNote: string) =>
  requestJson<PaymentDisputeReport>(`/api/admin/payment-disputes/${encodeURIComponent(reportId)}/review`, {
    method: "PATCH",
    ...jsonBody({ approved, adminNote }),
  });
