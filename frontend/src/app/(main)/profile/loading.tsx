import { ProfileSkeleton } from "@/components/ui/data-state";
import { PageHeading } from "@/components/ui/page-heading";

export default function ProfileLoading() {
  return (
    <div className="mx-auto min-h-[70vh] max-w-5xl px-5 py-12 sm:px-8">
      <PageHeading eyebrow="Tài khoản" title="Hồ sơ cá nhân" />
      <div className="mt-8"><ProfileSkeleton /></div>
    </div>
  );
}
