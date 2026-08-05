import type { Metadata } from "next";
import { CommunityFeedContent } from "@/components/community/community-feed-content";
import { getCurrentUser } from "@/lib/server/session";
import type { CommunityPostFilters } from "@/lib/api/types";

export const metadata: Metadata = {
  title: "Cộng đồng bóng đá",
  description: "Tìm đối thủ hoặc tuyển thêm cầu thủ cho lịch đặt sân đã xác nhận.",
};

export default async function CommunityPage({
  searchParams,
}: {
  searchParams: Promise<Record<string, string | string[] | undefined>>;
}) {
  const params = await searchParams;
  const user = await getCurrentUser();
  return (
    <CommunityFeedContent
      pageNumber={parsePage(single(params.page))}
      filters={parseFilters(params)}
      viewerId={user?.id ?? null}
      canCreate={user?.userType === "CLIENT" || user?.userType === "EMPLOYEE"}
    />
  );
}

function parsePage(value?: string) {
  const parsed = Number.parseInt(value ?? "1", 10);
  return Number.isFinite(parsed) && parsed > 0 ? parsed : 1;
}

function single(value: string | string[] | undefined) {
  return Array.isArray(value) ? value[0] : value;
}

function parseFilters(params: Record<string, string | string[] | undefined>): CommunityPostFilters {
  const value = (key: string) => single(params[key])?.trim() || undefined;
  const ownerId = value("ownerId");
  const applicantId = value("applicantId");
  const status = value("status");
  return {
    ownerId,
    applicantId,
    postType: value("postType") as CommunityPostFilters["postType"],
    skillLevel: value("skillLevel"),
    date: value("date"),
    fieldType: value("fieldType"),
    city: value("city"),
    district: value("district"),
    fieldName: value("fieldName"),
    status: (status === "all" ? "all" : status ?? (ownerId || applicantId ? "all" : "OPEN")) as CommunityPostFilters["status"],
    keyword: value("keyword"),
    sortBy: value("sortBy") === "asc" ? "asc" : "desc",
  };
}
