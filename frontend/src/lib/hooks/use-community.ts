"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import type {
  AdminModerationInput,
  CommunityPostFilters,
  CommunityReportReason,
  CommunityReportStatus,
  CreateCommunityPostInput,
  MatchEvaluationInput,
  UpdateCommunityPostInput,
} from "@/lib/api/types";
import {
  fetchCommunityReports,
  fetchCommunityPost,
  fetchCommunityPosts,
  submitAdminModeration,
  submitCommunityApplication,
  submitCommunityDecision,
  submitCommunityPost,
  submitCommunityPostAction,
  submitCommunityPostUpdate,
  submitCommunityReport,
  submitCommunityMatchEvaluation,
  submitCommunityWithdraw,
  submitOwnerHideCommunityPost,
} from "@/lib/client/community";
import { communityQueryKeys } from "@/lib/query-keys";

export function useCommunityPosts(page: number, size = 10, filters: CommunityPostFilters = {}) {
  return useQuery({
    queryKey: communityQueryKeys.list(page, size, filters),
    queryFn: () => fetchCommunityPosts(page, size, filters),
  });
}

export function useCommunityPost(id: string) {
  return useQuery({
    queryKey: communityQueryKeys.detail(id),
    queryFn: () => fetchCommunityPost(id),
  });
}

export function useCreateCommunityPost() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (input: CreateCommunityPostInput) => submitCommunityPost(input),
    retry: false,
    onSuccess: (post) => {
      queryClient.setQueryData(communityQueryKeys.detail(post.id), post);
      void queryClient.invalidateQueries({ queryKey: communityQueryKeys.all });
    },
  });
}

export function useUpdateCommunityPost(id: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (input: UpdateCommunityPostInput) => submitCommunityPostUpdate(id, input),
    retry: false,
    onSuccess: (post) => {
      queryClient.setQueryData(communityQueryKeys.detail(post.id), post);
      void queryClient.invalidateQueries({ queryKey: communityQueryKeys.all });
    },
  });
}

export function useCommunityPostAction(id: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (action: "close" | "full") => submitCommunityPostAction(id, action),
    retry: false,
    onSuccess: (post) => {
      queryClient.setQueryData(communityQueryKeys.detail(post.id), post);
      void queryClient.invalidateQueries({ queryKey: communityQueryKeys.all });
    },
  });
}

export function useApplyCommunityPost(id: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (body: Record<string, unknown>) => submitCommunityApplication(id, body),
    retry: false,
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: communityQueryKeys.detail(id) });
      await queryClient.refetchQueries({ queryKey: communityQueryKeys.detail(id), type: "active" });
      void queryClient.invalidateQueries({ queryKey: communityQueryKeys.all });
    },
    onError: (error) => {
      console.error("Error applying to community post:", error);
    },
  });
}

export function useWithdrawCommunityApplication(id: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: () => submitCommunityWithdraw(id),
    retry: false,
    onSuccess: () => void queryClient.invalidateQueries({ queryKey: communityQueryKeys.detail(id) }),
  });
}

export function useDecideCommunityApplication(id: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ applicationId, decision }: { applicationId: string; decision: "accept" | "reject" }) =>
      submitCommunityDecision(id, applicationId, decision),
    retry: false,
    onSuccess: () => void queryClient.invalidateQueries({ queryKey: communityQueryKeys.detail(id) }),
  });
}

export function useReportCommunityPost(id: string) {
  return useMutation({
    mutationFn: ({ reason, description }: { reason: CommunityReportReason; description?: string }) =>
      submitCommunityReport(id, reason, description),
    retry: false,
  });
}

export function useOwnerHideCommunityPost(id: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (reason: string) => submitOwnerHideCommunityPost(id, reason),
    retry: false,
    onSuccess: (post) => {
      queryClient.setQueryData(communityQueryKeys.detail(post.id), post);
      void queryClient.invalidateQueries({ queryKey: communityQueryKeys.all });
    },
  });
}

export function useSubmitMatchEvaluation(id: string) {
  return useMutation({
    mutationFn: (input: MatchEvaluationInput) => submitCommunityMatchEvaluation(id, input),
    retry: false,
  });
}

export function useCommunityReports(page: number, size = 20, status?: CommunityReportStatus) {
  return useQuery({
    queryKey: communityQueryKeys.reports(page, size, status),
    queryFn: () => fetchCommunityReports(page, size, status),
  });
}

export function useAdminModeration() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (input: AdminModerationInput) => submitAdminModeration(input),
    retry: false,
    onSuccess: () => void queryClient.invalidateQueries({ queryKey: communityQueryKeys.all }),
  });
}
