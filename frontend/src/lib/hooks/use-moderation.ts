"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import type { PaymentDisputeStatus } from "@/lib/api/types";
import {
  fetchAdminPaymentDisputes,
  fetchFieldViolations,
  fetchModerationAuditLogs,
  fetchNoShowReports,
} from "@/lib/client/moderation";
import { submitUserStatus } from "@/lib/client/users";
import { moderationQueryKeys, userQueryKeys } from "@/lib/query-keys";

export function useFieldViolations(fieldId: string, page = 0, size = 20) {
  return useQuery({
    queryKey: moderationQueryKeys.fieldViolations(fieldId, page, size),
    queryFn: () => fetchFieldViolations(fieldId, page, size),
    enabled: Boolean(fieldId),
  });
}

export function useNoShowReports(fieldId: string, page = 0, size = 20) {
  return useQuery({
    queryKey: moderationQueryKeys.noShowReports(fieldId, page, size),
    queryFn: () => fetchNoShowReports(fieldId, page, size),
    enabled: Boolean(fieldId),
  });
}

export function useModerationAuditLogs(fieldId: string, page = 0, size = 20) {
  return useQuery({
    queryKey: moderationQueryKeys.auditLogs(fieldId, page, size),
    queryFn: () => fetchModerationAuditLogs(fieldId, page, size),
    enabled: Boolean(fieldId),
  });
}

export function useAdminPaymentDisputes(
  page = 0,
  size = 20,
  filters: { status?: PaymentDisputeStatus; fieldIds?: string[] } = {},
) {
  return useQuery({
    queryKey: moderationQueryKeys.adminPaymentDisputes(page, size, filters),
    queryFn: () => fetchAdminPaymentDisputes(page, size, filters.status, filters.fieldIds ?? []),
  });
}

export function useAdminPlayerBan() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ userId, banned }: { userId: string; banned: boolean }) =>
      submitUserStatus(userId, banned ? "PLATFORM_BANNED" : "ACTIVE"),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: moderationQueryKeys.all });
      void queryClient.invalidateQueries({ queryKey: userQueryKeys.all });
    },
  });
}
