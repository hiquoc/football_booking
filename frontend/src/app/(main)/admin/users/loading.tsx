import { ListSkeleton } from "@/components/ui/data-state";
import { PageHeading } from "@/components/ui/page-heading";

export default function AdminUsersLoading() {
  return (
    <>
      <PageHeading
        eyebrow="Tài khoản"
        title="Quản lý người dùng"
        description="Danh sách tài khoản khách hàng, chủ sân và quản trị viên."
      />
      <div className="mt-8">
        <ListSkeleton />
      </div>
    </>
  );
}
