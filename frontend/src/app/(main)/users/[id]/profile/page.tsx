import type { Metadata } from "next";
import { ProfileContent } from "@/components/profile/profile-content";
import { getCurrentUser } from "@/lib/server/session";

export const metadata: Metadata = { title: "Hồ sơ cầu thủ" };

export default async function PublicProfilePage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = await params;
  const currentUser = await getCurrentUser();
  const isOwnProfile = id === currentUser?.id;

  return (
    <div className="mx-auto min-h-[70vh] w-full max-w-[90rem] px-5 py-10 sm:px-8">
      <ProfileContent userId={id} isOwnProfile={isOwnProfile} />
    </div>
  );
}
