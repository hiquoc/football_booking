"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import type { PageResponse, User } from "@/lib/api/types";
import {
  fetchCurrentUser,
  fetchEmployeeByPhone,
  fetchPublicProfile,
  fetchUserViolations,
  fetchUsers,
  submitUserRole,
  submitUserStatus,
} from "@/lib/client/users";
import { userQueryKeys } from "@/lib/query-keys";

export function useCurrentUser() {
  return useQuery({
    queryKey: userQueryKeys.me,
    queryFn: fetchCurrentUser,
    staleTime: 5 * 60 * 1000,
  });
}

export function useUsers(page: number, size = 10, phoneNumber = "") {
  return useQuery({
    queryKey: userQueryKeys.list(page, size, phoneNumber),
    queryFn: () => fetchUsers(page, size, phoneNumber),
  });
}

export function useUserViolations(id: string, enabled = true) {
  return useQuery({
    queryKey: userQueryKeys.violations(id),
    queryFn: () => fetchUserViolations(id),
    enabled: enabled && Boolean(id),
  });
}

export function useEmployeeByPhone(phoneNumber: string, enabled: boolean) {
  return useQuery({
    queryKey: [...userQueryKeys.all, "employee-by-phone", phoneNumber],
    queryFn: () => fetchEmployeeByPhone(phoneNumber),
    enabled: enabled && phoneNumber.trim().length > 0,
    retry: false,
  });
}

export function usePublicProfile(id: string, enabled = true) {
  return useQuery({
    queryKey: userQueryKeys.profile(id),
    queryFn: () => fetchPublicProfile(id),
    enabled: enabled && Boolean(id),
    staleTime: 5 * 60 * 1000,
  });
}

export function useUpdateUserRole(page: number, size = 10, phoneNumber = "") {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, userType }: { id: string; userType: User["userType"] }) =>
      submitUserRole(id, userType),
    onMutate: async ({ id, userType }) => {
      await queryClient.cancelQueries({ queryKey: userQueryKeys.all });
      const snapshot = queryClient.getQueriesData({ queryKey: userQueryKeys.all });
      queryClient.setQueriesData<PageResponse<User>>({ queryKey: userQueryKeys.all }, (old) => old ? { ...old, content: old.content.map((user) => user.id === id ? { ...user, userType } : user) } : old);
      return snapshot;
    },
    onError: (_error, _variables, snapshot) => snapshot?.forEach(([key, data]) => queryClient.setQueryData(key, data)),
    onSuccess: () =>
      queryClient.invalidateQueries({ queryKey: userQueryKeys.list(page, size, phoneNumber) }),
  });
}

export function useUpdateUserStatus(page: number, size = 10, phoneNumber = "") {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, status }: { id: string; status: "ACTIVE" | "PLATFORM_BANNED" }) =>
      submitUserStatus(id, status),
    onMutate: async ({ id, status }) => {
      await queryClient.cancelQueries({ queryKey: userQueryKeys.all });
      const snapshot = queryClient.getQueriesData({ queryKey: userQueryKeys.all });
      queryClient.setQueriesData<PageResponse<User>>({ queryKey: userQueryKeys.all }, (old) =>
        old
          ? {
              ...old,
              content: old.content.map((user) =>
                user.id === id
                  ? {
                      ...user,
                      status,
                      isBookingBanned: status === "PLATFORM_BANNED",
                      isPermanentBan: status === "PLATFORM_BANNED",
                      banExpiresAt: null,
                    }
                  : user,
              ),
            }
          : old,
      );
      return snapshot;
    },
    onError: (_error, _variables, snapshot) => snapshot?.forEach(([key, data]) => queryClient.setQueryData(key, data)),
    onSuccess: () =>
      queryClient.invalidateQueries({ queryKey: userQueryKeys.list(page, size, phoneNumber) }),
  });
}
