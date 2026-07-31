import type { Metadata } from "next";
import { CommunityFeedContent } from "@/components/community/community-feed-content";
import { getCurrentUser } from "@/lib/server/session";
import type { CommunityPostFilters } from "@/lib/api/types";

export const metadata: Metadata = {
  title: "Cong dong bong da",
  description: "Tim doi thu hoac tuyen them cau thu cho lich dat san da xac nhan.",
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
    date: value("date") ?? tomorrowDate(),
    fieldType: value("fieldType"),
    city: value("city"),
    district: value("district"),
    fieldName: value("fieldName"),
    status: (status === "all" ? "all" : status ?? (ownerId || applicantId ? "all" : "OPEN")) as CommunityPostFilters["status"],
    keyword: value("keyword"),
    sortBy: value("sortBy") === "newest" ? "newest" : "upcoming",
  };
}

function tomorrowDate() {
  const date = new Date();
  date.setDate(date.getDate() + 1);
  return [
    date.getFullYear(),
    String(date.getMonth() + 1).padStart(2, "0"),
    String(date.getDate()).padStart(2, "0"),
  ].join("-");
}
