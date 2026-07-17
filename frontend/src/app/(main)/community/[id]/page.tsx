import type { Metadata } from "next";
import { CommunityDetailContent } from "@/components/community/community-detail-content";
import { getCurrentUser } from "@/lib/server/session";
import { getMyPublicProfile } from "@/lib/server/users";

export const metadata: Metadata = {
  title: "Chi tiet bai dang cong dong",
};

export default async function CommunityDetailPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  const user = await getCurrentUser();
  const profile = user?.userType === "CLIENT" ? await getMyPublicProfile().catch(() => null) : null;
  return <CommunityDetailContent postId={id} viewer={user} profile={profile} />;
}
