import { Suspense } from "react";
import { AdminNav } from "@/components/admin/admin-nav";
import { AccessDenied } from "@/components/ui/access-denied";
import { requireUser } from "@/lib/server/guards";

export default function AdminLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <div>
      <AdminNav />
      <div className="mx-auto w-full max-w-[90rem] px-5 py-10 sm:px-8">
        <Suspense fallback={children}>
          <AdminAccessGate>{children}</AdminAccessGate>
        </Suspense>
      </div>
    </div>
  );
}

async function AdminAccessGate({
  children,
}: {
  children: React.ReactNode;
}) {
  const user = await requireUser();
  if (user.userType !== "ADMIN") return <AccessDenied />;
  return children;
}
