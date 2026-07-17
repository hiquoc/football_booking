import type { Metadata } from "next";
import { CommunityModerationPanel } from "@/components/admin/community-moderation-panel";

export const metadata: Metadata = {
  title: "Kiểm duyệt cộng đồng",
};

export default function AdminCommunityModerationPage() {
  return <CommunityModerationPanel />;
}
