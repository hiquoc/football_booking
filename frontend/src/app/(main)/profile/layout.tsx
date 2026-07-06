import { requireUser } from "@/lib/server/guards";
export default async function ProfileLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  await requireUser();
  return children;
}
