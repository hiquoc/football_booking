"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import type { FieldTypeInput } from "@/lib/api/types";
import {
  fetchFieldTypes,
  fetchSubFieldTypes,
  submitFieldType,
  submitFieldTypeDelete,
  submitFieldTypeUpdate,
} from "@/lib/client/field-types";
import { fieldTypeQueryKeys } from "@/lib/query-keys";

export function useFieldTypes() {
  return useQuery({
    queryKey: fieldTypeQueryKeys.all,
    queryFn: fetchFieldTypes,
    staleTime: 31_536_000_000,
    gcTime: 31_536_000_000,
  });
}

export function useSubFieldTypes() {
  return useQuery({
    queryKey: fieldTypeQueryKeys.subFieldTypes,
    queryFn: fetchSubFieldTypes,
    staleTime: 31_536_000_000,
    gcTime: 31_536_000_000,
  });
}

export function useCreateFieldType() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: submitFieldType,
    onSuccess: () =>
      queryClient.invalidateQueries({ queryKey: fieldTypeQueryKeys.all }),
  });
}

export function useUpdateFieldType() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, input }: { id: number; input: FieldTypeInput }) =>
      submitFieldTypeUpdate(id, input),
    onSuccess: () =>
      queryClient.invalidateQueries({ queryKey: fieldTypeQueryKeys.all }),
  });
}

export function useDeleteFieldType() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: submitFieldTypeDelete,
    onSuccess: () =>
      queryClient.invalidateQueries({ queryKey: fieldTypeQueryKeys.all }),
  });
}
