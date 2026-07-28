"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import type { User } from "@/lib/api/types";
import { fetchEmployeeByPhone, fetchPublicProfile, fetchUsers, submitUserRole } from "@/lib/client/users";
import { userQueryKeys } from "@/lib/query-keys";

export function useUsers(page: number, size = 10) {
  return useQuery({
    queryKey: userQueryKeys.list(page, size),
    queryFn: () => fetchUsers(page, size),
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

export function useUpdateUserRole(page: number, size = 10) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, userType }: { id: string; userType: User["userType"] }) =>
      submitUserRole(id, userType),
    onMutate: async ({ id, userType }) => {
      await queryClient.cancelQueries({ queryKey: userQueryKeys.all });
      const snapshot = queryClient.getQueriesData({ queryKey: userQueryKeys.all });
      queryClient.setQueriesData<{ content: User[] }>({ queryKey: userQueryKeys.all }, (old) => old ? { ...old, content: old.content.map((user) => user.id === id ? { ...user, userType } : user) } : old);
      return snapshot;
    },
    onError: (_error, _variables, snapshot) => snapshot?.forEach(([key, data]) => queryClient.setQueryData(key, data)),
    onSuccess: () =>
      queryClient.invalidateQueries({ queryKey: userQueryKeys.list(page, size) }),
  });
}
