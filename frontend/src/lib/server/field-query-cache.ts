import "server-only";

import { QueryClient } from "@tanstack/react-query";
import type { FieldCardFilters, FieldStatus, SubField } from "@/lib/api/types";
import {
  bookingQueryKeys,
  fieldQueryKeys,
  ownerFieldQueryKeys,
} from "@/lib/query-keys";
import { getAvailability } from "./bookings";
import {
  getField,
  getFieldCards,
  getFieldDetails,
  getFields,
  getOwnerFields,
  getSubFields,
} from "./fields";

export async function prefetchOwnerFields(page: number, size = 10) {
  const queryClient = new QueryClient();
  await queryClient.prefetchQuery({
    queryKey: ownerFieldQueryKeys.list(page, size),
    queryFn: () => getOwnerFields(page, size),
  });
  return queryClient;
}

export async function prefetchFields(
  page: number,
  size = 9,
  status?: FieldStatus,
) {
  const queryClient = new QueryClient();

  await queryClient.prefetchQuery({
    queryKey: fieldQueryKeys.list(page, size, status),
    queryFn: () => getFields(page, size, status),
  });

  return queryClient;
}

export async function prefetchFieldCards(
  page: number,
  size = 9,
  filters: FieldCardFilters = {},
) {
  const queryClient = new QueryClient();
  await queryClient.prefetchQuery({
    queryKey: fieldQueryKeys.cards(page, size, filters),
    queryFn: () => getFieldCards(page, size, filters),
  });
  return queryClient;
}

export async function prefetchFieldDetails(id: string) {
  const queryClient = new QueryClient();
  await queryClient.fetchQuery({
    queryKey: fieldQueryKeys.details(id),
    queryFn: () => getFieldDetails(id),
  });

  return queryClient;
}

export async function prefetchFieldBooking(id: string, date: string) {
  const queryClient = new QueryClient();

  await Promise.all([
    queryClient.fetchQuery({
      queryKey: fieldQueryKeys.detail(id),
      queryFn: () => getField(id),
    }),
    queryClient.fetchQuery({
      queryKey: fieldQueryKeys.subFields(id),
      queryFn: () => getSubFields(id),
    }),
  ]);

  const subFields =
    queryClient.getQueryData<SubField[]>(fieldQueryKeys.subFields(id)) ?? [];

  await Promise.all(
    subFields
      .filter((subField) => subField.active)
      .map((subField) =>
        queryClient.prefetchQuery({
          queryKey: bookingQueryKeys.availability(subField.id, date),
          queryFn: () => getAvailability(subField.id, date),
        }),
      ),
  );

  return queryClient;
}
