import type { Metadata } from "next";
import { redirect } from "next/navigation";
import { CommunityCreateContent } from "@/components/community/community-create-content";
import { getCurrentUser } from "@/lib/server/session";
import { getMyProfile } from "@/lib/server/users";

export const metadata: Metadata = {
  title: "Đăng bài cộng đồng",
};

export default async function NewCommunityPostPage() {
  const user = await getCurrentUser();
  if (!user) redirect("/auth/login");
  if (user.userType !== "CLIENT" && user.userType !== "EMPLOYEE") redirect("/community");
  const profile = await getMyProfile().catch(() => null);
  return <CommunityCreateContent profile={profile} />;
}
