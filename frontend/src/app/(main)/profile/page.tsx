import type { Metadata } from "next";
import { ProfileContent } from "@/components/profile/profile-content";
import { PageHeading } from "@/components/ui/page-heading";
import { getCurrentUser } from "@/lib/server/session";

export const metadata: Metadata = { title: "Hồ sơ cầu thủ" };

export default async function ProfilePage({
  searchParams,
}: {
  searchParams: Promise<{ userId?: string }>;
}) {
  const { userId } = await searchParams;
  const currentUser = await getCurrentUser();
  const isOwnProfile = !userId || userId === currentUser?.id;

  return (
    <div className="mx-auto min-h-[70vh] max-w-5xl px-5 py-12 sm:px-8">
      <PageHeading eyebrow="Cầu thủ" title="Hồ sơ cầu thủ" />
      <div className="mt-8">
        <ProfileContent userId={userId} isOwnProfile={isOwnProfile} />
      </div>
    </div>
  );
}
