import { requireUser } from "@/lib/server/guards";
export default async function NotificationsLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  await requireUser();
  return children;
}
