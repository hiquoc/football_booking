"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  fetchField,
  fetchFieldOperatingHours,
  fetchFields,
  fetchFieldCards,
  fetchFieldDetails,
  fetchSubFields,
  submitFieldReview,
  submitFieldStatus,
} from "@/lib/client/fields";
import { fieldQueryKeys } from "@/lib/query-keys";
import type { Field, FieldCardFilters, FieldStatus, PageResponse } from "@/lib/api/types";

export function useFields(page: number, size = 9, status?: FieldStatus) {
  return useQuery({
    queryKey: fieldQueryKeys.list(page, size, status),
    queryFn: () => fetchFields(page, size, status),
  });
}

export function useUpdateFieldStatus(id: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (status: FieldStatus) => submitFieldStatus(id, status),
    onMutate: async (status) => {
      await queryClient.cancelQueries({ queryKey: fieldQueryKeys.all });
      const snapshot = queryClient.getQueriesData({ queryKey: fieldQueryKeys.all });
      queryClient.setQueriesData<PageResponse<Field>>({ queryKey: fieldQueryKeys.all }, (old) => old ? { ...old, content: old.content.map((field) => field.id === id ? { ...field, status } : field) } : old);
      queryClient.setQueryData<Field>(fieldQueryKeys.detail(id), (old) => old ? { ...old, status } : old);
      return snapshot;
    },
    onError: (_error, _status, snapshot) => snapshot?.forEach(([key, data]) => queryClient.setQueryData(key, data)),
    onSuccess: (field) => {
      queryClient.setQueryData(fieldQueryKeys.detail(id), field);
      void queryClient.invalidateQueries({ queryKey: fieldQueryKeys.all });
    },
  });
}

export function useFieldCards(
  page: number,
  size = 9,
  filters: FieldCardFilters = {},
) {
  return useQuery({
    queryKey: fieldQueryKeys.cards(page, size, filters),
    queryFn: () => fetchFieldCards(page, size, filters),
  });
}

export function useFieldDetails(id: string) {
  return useQuery({
    queryKey: fieldQueryKeys.details(id),
    queryFn: () => fetchFieldDetails(id),
  });
}

export function useFieldBookingData(id: string) {
  const field = useQuery({
    queryKey: fieldQueryKeys.detail(id),
    queryFn: () => fetchField(id),
  });
  const subFields = useQuery({
    queryKey: fieldQueryKeys.subFields(id),
    queryFn: () => fetchSubFields(id),
  });
  return { field, subFields };
}

export function useFieldEditorData(id: string) {
  const field = useQuery({
    queryKey: fieldQueryKeys.detail(id),
    queryFn: () => fetchField(id),
  });
  const operatingHours = useQuery({
    queryKey: fieldQueryKeys.operatingHours(id),
    queryFn: () => fetchFieldOperatingHours(id),
  });
  return { field, operatingHours };
}

export function useCreateReview(fieldId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ rating, comment }: { rating: number; comment?: string }) =>
      submitFieldReview(fieldId, rating, comment),
    onSuccess: () => {
      void queryClient.invalidateQueries({
        queryKey: fieldQueryKeys.details(fieldId),
      });
    },
  });
}
