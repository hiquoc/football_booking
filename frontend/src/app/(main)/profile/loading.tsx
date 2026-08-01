import { ProfileSkeleton } from "@/components/ui/data-state";

export default function ProfileLoading() {
  return (
    <div className="mx-auto min-h-[70vh] w-full max-w-[90rem] px-5 py-10 sm:px-8">
      <ProfileSkeleton />
    </div>
  );
}
