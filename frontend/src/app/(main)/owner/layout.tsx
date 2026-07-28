import { Suspense } from "react";
import { OwnerNav } from "@/components/owner/owner-nav";
import { AccessDenied } from "@/components/ui/access-denied";
import { requireUser } from "@/lib/server/guards";

export default function OwnerLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <>
      <OwnerNav />
      <div className="mx-auto w-full max-w-[90rem] px-5 py-8 sm:px-8">
        <Suspense fallback={children}>
          <OwnerAccessGate>{children}</OwnerAccessGate>
        </Suspense>
      </div>
    </>
  );
}

async function OwnerAccessGate({
  children,
}: {
  children: React.ReactNode;
}) {
  const user = await requireUser();

  if (user.userType !== "OWNER" && user.userType !== "EMPLOYEE") {
    return <AccessDenied />;
  }

  return children;
}
