"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import type { UpdateProfileInput, User } from "@/lib/api/types";
import { fetchMyProfile, submitAvatar, submitProfileUpdate } from "@/lib/client/users";
import { userQueryKeys } from "@/lib/query-keys";

export function useProfile() {
  return useQuery({ queryKey: userQueryKeys.me, queryFn: fetchMyProfile });
}

export function useUploadAvatar() {
  const queryClient = useQueryClient();
  return useMutation({ mutationFn: (file: File) => submitAvatar(file),
    onSuccess: (user) => queryClient.setQueryData(userQueryKeys.me, user) });
}

export function useUpdateProfile() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (input: UpdateProfileInput) => submitProfileUpdate(input),
    onMutate: async (input) => {
      await queryClient.cancelQueries({ queryKey: userQueryKeys.me });
      const previous = queryClient.getQueryData(userQueryKeys.me);
      queryClient.setQueryData(userQueryKeys.me, (old: User | undefined) => old ? { ...old, ...input } : old);
      return previous;
    },
    onError: (_error, _input, previous) => queryClient.setQueryData(userQueryKeys.me, previous),
    onSuccess: (user) => queryClient.setQueryData(userQueryKeys.me, user),
  });
}
