import type { Metadata } from "next";
import { redirect } from "next/navigation";
import { CommunityFeedContent } from "@/components/community/community-feed-content";
import type { CommunityPostFilters } from "@/lib/api/types";
import { getCurrentUser } from "@/lib/server/session";

export const metadata: Metadata = {
  title: "Bài đăng của tôi",
  description: "Các bài đăng cộng đồng mà tôi đã tạo.",
};

export default async function MyCommunityPostsPage({
  searchParams,
}: {
  searchParams: Promise<Record<string, string | string[] | undefined>>;
}) {
  const params = await searchParams;
  const user = await getCurrentUser();
  if (!user || (user.userType !== "CLIENT" && user.userType !== "EMPLOYEE")) redirect("/community");

  return (
    <CommunityFeedContent
      pageNumber={parsePage(single(params.page))}
      filters={{ ...parseFilters(params), ownerId: user.id }}
      viewerId={user.id}
      canCreate
      basePath="/community/my-posts"
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
  const status = value("status");
  return {
    postType: value("postType") as CommunityPostFilters["postType"],
    skillLevel: value("skillLevel"),
    date: value("date"),
    fieldType: value("fieldType"),
    city: value("city"),
    district: value("district"),
    fieldName: value("fieldName"),
    status: (status === "all" ? "all" : status ?? "all") as CommunityPostFilters["status"],
    keyword: value("keyword"),
    sortBy: value("sortBy") === "upcoming" ? "upcoming" : "newest",
  };
}
