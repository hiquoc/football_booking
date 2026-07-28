import { requireUser } from "@/lib/server/guards";
import { AccessDenied } from "@/components/ui/access-denied";

export default async function BookingsLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  const user = await requireUser();
  if (user.userType !== "CLIENT" && user.userType !== "EMPLOYEE") return <AccessDenied />;
  return children;
}
