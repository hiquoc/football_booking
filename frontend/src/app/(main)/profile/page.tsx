import type { Metadata } from "next";
import { ProfileContent } from "@/components/profile/profile-content";
import { PageHeading } from "@/components/ui/page-heading";
export const metadata: Metadata = { title: "Hồ sơ cá nhân" };
export default function ProfilePage() {
  return (
    <div className="mx-auto min-h-[70vh] max-w-5xl px-5 py-12 sm:px-8">
      <PageHeading eyebrow="Tài khoản" title="Hồ sơ cá nhân" />
      <div className="mt-8">
        <ProfileContent />
      </div>
    </div>
  );
}
