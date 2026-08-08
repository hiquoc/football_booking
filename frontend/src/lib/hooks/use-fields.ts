"use client";

import { useMutation, useQuery, useQueryClient, type QueryKey } from "@tanstack/react-query";
import {
  addFavoriteField,
  fetchField,
  fetchFieldOperatingHours,
  fetchFields,
  fetchFieldCards,
  fetchFieldDetails,
  fetchFieldReviews,
  fetchFavoriteFields,
  fetchAdminFieldSearch,
  removeFavoriteField,
  fetchSubFields,
  fetchSubFieldFilterOptions,
  submitFieldReview,
  submitFieldStatus,
} from "@/lib/client/fields";
import { fieldQueryKeys } from "@/lib/query-keys";
import { useToast } from "@/components/providers/toast-provider";
import type { Field, FieldCardData, FieldCardFilters, FieldDetails, FieldStatus, PageResponse, Review } from "@/lib/api/types";

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
      queryClient.setQueriesData<PageResponse<Field>>({ queryKey: fieldQueryKeys.all }, (old) => old ? { ...old, content: old.content?.map((field) => field.id === id ? { ...field, status } : field) } : old);
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

export function useFavoriteFields(page: number, size = 4) {
  return useQuery({
    queryKey: fieldQueryKeys.favorites(page, size),
    queryFn: () => fetchFavoriteFields(page, size),
  });
}

export function useToggleFavoriteField(fieldId: string) {
  const queryClient = useQueryClient();
  const { showToast } = useToast();

  type Snapshot = Array<[QueryKey, unknown]>;

  return useMutation<Field | void, Error, boolean, Snapshot>({
    mutationFn: (saved: boolean) =>
      saved ? addFavoriteField(fieldId) : removeFavoriteField(fieldId),
    onMutate: async (saved) => {
      await queryClient.cancelQueries({ queryKey: fieldQueryKeys.all });
      const snapshot = queryClient.getQueriesData({ queryKey: fieldQueryKeys.all }) as Snapshot;

      const patchField = <T extends { id: string; isSaved?: boolean; isFavorite?: boolean }>(item: T): T =>
        item.id === fieldId ? { ...item, isSaved: saved, isFavorite: saved } : item;

      queryClient.setQueriesData<PageResponse<Field>>(
        { queryKey: fieldQueryKeys.all },
        (old) => old && Array.isArray(old.content)
          ? { ...old, content: old.content.map(patchField) }
          : old,
      );
      queryClient.setQueriesData<PageResponse<FieldCardData>>(
        { queryKey: fieldQueryKeys.all },
        (old) => old && Array.isArray(old.content)
          ? { ...old, content: old.content.map(patchField) }
          : old,
      );
      queryClient.setQueryData<Field>(fieldQueryKeys.detail(fieldId), (old) =>
        old ? { ...old, isSaved: saved, isFavorite: saved } : old,
      );
      queryClient.setQueryData<FieldDetails>(fieldQueryKeys.details(fieldId), (old) =>
        old ? { ...old, field: { ...old.field, isSaved: saved, isFavorite: saved } } : old,
      );

      return snapshot;
    },
    onError: (_error, _saved, snapshot) => {
      snapshot?.forEach(([key, data]) => queryClient.setQueryData(key, data));
      showToast("Could not update saved field. Please try again.", "error");
    },
    onSuccess: (_result, saved) => {
      showToast(
        saved ? "Field saved." : "Field removed from saved fields.",
        "success",
      );
      void queryClient.invalidateQueries({ queryKey: fieldQueryKeys.all });
    },
  });
}

export function useFieldDetails(id: string) {
  return useQuery({
    queryKey: fieldQueryKeys.details(id),
    queryFn: () => fetchFieldDetails(id),
  });
}

export function useFieldReviews(id: string, page = 0, size = 6, initialData?: PageResponse<Review>) {
  return useQuery({
    queryKey: fieldQueryKeys.reviewPage(id, page, size),
    queryFn: () => fetchFieldReviews(id, page, size),
    initialData,
  });
}

export function useSubFieldFilterOptions(search = "", enabled = true) {
  return useQuery({
    queryKey: fieldQueryKeys.subFieldFilterOptions(search),
    queryFn: () => fetchSubFieldFilterOptions(search),
    enabled,
    staleTime: 5 * 60 * 1000,
  });
}

export function useAdminFieldSearch(keyword = "", enabled = true) {
  return useQuery({
    queryKey: fieldQueryKeys.adminSearch(keyword),
    queryFn: () => fetchAdminFieldSearch(keyword),
    enabled,
    staleTime: 60 * 1000,
  });
}

export function useFieldBookingData(id: string) {
  const field = useQuery({
    queryKey: fieldQueryKeys.detail(id),
    queryFn: () => fetchField(id),
  });
  const operatingHours = useQuery({
    queryKey: fieldQueryKeys.operatingHours(id),
    queryFn: () => fetchFieldOperatingHours(id),
  });
  const subFields = useQuery({
    queryKey: fieldQueryKeys.subFields(id),
    queryFn: () => fetchSubFields(id),
  });
  return { field, operatingHours, subFields };
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
      void queryClient.invalidateQueries({
        queryKey: fieldQueryKeys.reviews(fieldId),
      });
    },
  });
}
