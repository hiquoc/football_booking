"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import type { PublicProfile, UpdateProfileInput } from "@/lib/api/types";
import { fetchCurrentUser, fetchMyProfile, fetchPublicProfile, submitAvatar, submitProfileUpdate, submitTeamPhoto } from "@/lib/client/users";
import { userQueryKeys } from "@/lib/query-keys";

export function useProfile(userId?: string) {
  return useQuery({
    queryKey: userId ? userQueryKeys.profile(userId) : userQueryKeys.me,
    queryFn: () => userId ? fetchPublicProfile(userId) : fetchMyProfile(),
  });
}

export function useCurrentUser() {
  return useQuery({ queryKey: userQueryKeys.mePrivate, queryFn: fetchCurrentUser });
}

export function useUploadAvatar() {
  const queryClient = useQueryClient();
  return useMutation({ mutationFn: (file: File) => submitAvatar(file),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: userQueryKeys.me }) });
}

export function useUploadTeamPhoto() {
  const queryClient = useQueryClient();
  return useMutation({ mutationFn: (file: File) => submitTeamPhoto(file),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: userQueryKeys.me }) });
}

export function useUpdateProfile() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (input: UpdateProfileInput) => submitProfileUpdate(input),
    onMutate: async (input) => {
      await queryClient.cancelQueries({ queryKey: userQueryKeys.me });
      const previous = queryClient.getQueryData(userQueryKeys.me);
      queryClient.setQueryData(userQueryKeys.me, (old: PublicProfile | undefined) => old ? {
        ...old,
        personal: { ...old.personal, ...input },
      } : old);
      return previous;
    },
    onError: (_error, _input, previous) => queryClient.setQueryData(userQueryKeys.me, previous),
    onSuccess: (user) => queryClient.setQueryData(userQueryKeys.me, user),
  });
}
