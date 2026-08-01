import type { Metadata } from "next";
import Link from "next/link";
import { Plus } from "lucide-react";
import { OwnerFieldsPanel } from "@/components/owner/owner-fields-panel";
import { PageHeading } from "@/components/ui/page-heading";
import { BackLink } from "@/components/ui/back-link";
import { requireUser } from "@/lib/server/guards";

export const metadata: Metadata = { title: "Sân của tôi" };

export default async function OwnerFieldsPage() {
  const user = await requireUser();
  const role = user.userType === "EMPLOYEE" ? "EMPLOYEE" : "OWNER";
  return (
    <>
      <BackLink href="/owner" className="mb-5">
        Quản lý sân
      </BackLink>
      <PageHeading
        eyebrow="Địa điểm"
        title="Sân của tôi"
        action={
          <Link
            href="/owner/fields/new"
            className="inline-flex items-center gap-2 rounded-full bg-green-600 px-5 py-3 text-sm font-black text-white"
          >
            <Plus className="size-4" /> Thêm sân
          </Link>
        }
      />
      <div className="mt-8">
        <OwnerFieldsPanel role={role} />
      </div>
    </>
  );
}
