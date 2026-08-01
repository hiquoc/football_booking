import type { Metadata } from "next";
import { ProfileContent } from "@/components/profile/profile-content";
import { getCurrentUser } from "@/lib/server/session";

export const metadata: Metadata = { title: "Hồ sơ cá nhân" };

export default async function ProfilePage({
  searchParams,
}: {
  searchParams: Promise<{ userId?: string }>;
}) {
  const { userId } = await searchParams;
  const currentUser = await getCurrentUser();
  const isOwnProfile = !userId || userId === currentUser?.id;

  return (
    <div className="mx-auto min-h-[70vh] w-full max-w-[90rem] px-5 py-10 sm:px-8">
      <ProfileContent userId={userId} isOwnProfile={isOwnProfile} />
    </div>
  );
}
