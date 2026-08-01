"use client";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import type { Field, FieldImage, FieldInput, SubFieldInput } from "@/lib/api/types";
import {
  fetchAssignedFields,
  fetchClosures,
  fetchFieldEmployees,
  fetchManagedFields,
  fetchOwnerFields,
  submitClosure,
  submitClosureDelete,
  submitClosureUpdate,
  submitField,
  submitFieldEmployee,
  submitFieldEmployeeRemoval,
  submitFieldUpdate,
  submitImageOrderChange,
  submitImageDelete,
  submitImages,
  submitSubField,
  submitSubFieldDelete,
  submitSubFieldUpdate,
} from "@/lib/client/owner-fields";
import { fieldQueryKeys, ownerFieldQueryKeys } from "@/lib/query-keys";
export function useOwnerFields(page: number, size = 10) {
  return useQuery({
    queryKey: ownerFieldQueryKeys.list(page, size),
    queryFn: () => fetchOwnerFields(page, size),
    staleTime: 60 * 1000,
  });
}
export function useManagedFields(role: "OWNER" | "EMPLOYEE", page: number, size = 10) {
  return useQuery({
    queryKey: role === "EMPLOYEE" ? ownerFieldQueryKeys.assigned(page, size) : ownerFieldQueryKeys.list(page, size),
    queryFn: () => role === "EMPLOYEE" ? fetchAssignedFields(page, size) : fetchOwnerFields(page, size),
    staleTime: 60 * 1000,
  });
}
export function useCurrentManagedFields(page: number, size = 10) {
  return useQuery({
    queryKey: ownerFieldQueryKeys.managed(page, size),
    queryFn: () => fetchManagedFields(page, size),
    staleTime: 60 * 1000,
  });
}
export function useAssignedFields(page: number, size = 10) {
  return useQuery({
    queryKey: ownerFieldQueryKeys.assigned(page, size),
    queryFn: () => fetchAssignedFields(page, size),
    staleTime: 60 * 1000,
  });
}
export function useFieldEmployees(fieldId: string) {
  return useQuery({
    queryKey: ownerFieldQueryKeys.employees(fieldId),
    queryFn: () => fetchFieldEmployees(fieldId),
    enabled: Boolean(fieldId),
  });
}
export function useAssignFieldEmployee(fieldId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (employeeId: string) => submitFieldEmployee(fieldId, employeeId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ownerFieldQueryKeys.employees(fieldId) }),
  });
}
export function useRemoveFieldEmployee(fieldId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (employeeId: string) => submitFieldEmployeeRemoval(fieldId, employeeId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ownerFieldQueryKeys.employees(fieldId) }),
  });
}
export function useCreateField() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (input: FieldInput) => submitField(input),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: fieldQueryKeys.all });
      void queryClient.invalidateQueries({ queryKey: ownerFieldQueryKeys.all });
    },
  });
}
export function useUpdateField(id: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (input: FieldInput) => submitFieldUpdate(id, input),
    onSuccess: (field) => {
      queryClient.setQueryData(fieldQueryKeys.detail(id), field);
      void queryClient.invalidateQueries({
        queryKey: fieldQueryKeys.operatingHours(id),
      });
      void queryClient.invalidateQueries({ queryKey: ownerFieldQueryKeys.all });
    },
  });
}
export function useCreateSubField(fieldId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (input: SubFieldInput) => submitSubField(fieldId, input),
    onSuccess: () =>
      queryClient.invalidateQueries({
        queryKey: fieldQueryKeys.subFields(fieldId),
      }),
  });
}
export function useDeleteSubField(fieldId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => submitSubFieldDelete(fieldId, id),
    onSuccess: () =>
      queryClient.invalidateQueries({
        queryKey: fieldQueryKeys.subFields(fieldId),
      }),
  });
}
export function useUpdateSubField(fieldId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, input }: { id: string; input: SubFieldInput }) =>
      submitSubFieldUpdate(fieldId, id, input),
    onSuccess: () =>
      queryClient.invalidateQueries({
        queryKey: fieldQueryKeys.subFields(fieldId),
      }),
  });
}
export function useClosures(fieldId: string, subFieldId: string) {
  return useQuery({
    queryKey: ownerFieldQueryKeys.closures(fieldId, subFieldId),
    queryFn: () => fetchClosures(fieldId, subFieldId),
    enabled: Boolean(subFieldId),
  });
}
export function useCreateClosure(fieldId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (input: {
      subFieldIds: string[];
      startDate: string;
      endDate: string;
      reason: string;
    }) => submitClosure(fieldId, input),
    onSuccess: () =>
      queryClient.invalidateQueries({
        queryKey: [...ownerFieldQueryKeys.all, fieldId, "closures"],
      }),
  });
}
export function useDeleteClosure(fieldId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => submitClosureDelete(fieldId, id),
    onSuccess: () =>
      queryClient.invalidateQueries({
        queryKey: [...ownerFieldQueryKeys.all, fieldId, "closures"],
      }),
  });
}
export function useUpdateClosure(fieldId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({
      id,
      input,
    }: {
      id: string;
      input: {
        subFieldIds: string[];
        startDate: string;
        endDate: string;
        reason: string;
      };
    }) => submitClosureUpdate(fieldId, id, input),
    onSuccess: () =>
      queryClient.invalidateQueries({
        queryKey: [...ownerFieldQueryKeys.all, fieldId, "closures"],
      }),
  });
}
export function useUploadFieldImages(fieldId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (files: FileList) => submitImages(fieldId, files),
    onSettled: () => {
      void queryClient.invalidateQueries({
        queryKey: fieldQueryKeys.detail(fieldId),
      });
    },
  });
}
export function useDeleteFieldImage(fieldId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => submitImageDelete(fieldId, id),
    onSuccess: () =>
      queryClient.invalidateQueries({
        queryKey: fieldQueryKeys.detail(fieldId),
      }),
  });
}
export function useChangeFieldImageOrder(fieldId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (imageIds: number[]) => submitImageOrderChange(fieldId, imageIds),
    onSuccess: (images: FieldImage[]) => {
      queryClient.setQueryData(fieldQueryKeys.detail(fieldId), (old: Field | undefined) => {
        if (!old) return old;
        return { ...old, images };
      });
    },
  });
}
