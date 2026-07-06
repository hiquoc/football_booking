import type { Metadata } from "next";
import { dehydrate, HydrationBoundary } from "@tanstack/react-query";
import { notFound } from "next/navigation";
import { FieldDetailContent } from "@/components/fields/field-detail-content";
import { ApiError } from "@/lib/server/gateway";
import { prefetchFieldDetails } from "@/lib/server/field-query-cache";
import { getCurrentUser } from "@/lib/server/session";

export const metadata: Metadata = { title: "Chi tiết sân" };

export default async function FieldDetailPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = await params;
  const user = await getCurrentUser();
  let queryClient;

  try {
    queryClient = await prefetchFieldDetails(id);
  } catch (error) {
    if (error instanceof ApiError && error.status === 404) notFound();
    throw error;
  }

  return (
    <HydrationBoundary state={dehydrate(queryClient)}>
      <FieldDetailContent
        fieldId={id}
        viewerRole={user?.userType ?? null}
        viewerUserId={user?.id ?? null}
      />
    </HydrationBoundary>
  );
}
