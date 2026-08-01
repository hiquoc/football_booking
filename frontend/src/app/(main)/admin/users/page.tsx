import { AdminUsersPanel } from "@/components/admin/admin-users-panel";
import { BackLink } from "@/components/ui/back-link";
import { PageHeading } from "@/components/ui/page-heading";

export default function AdminUsersPage() {
  return (
    <>
      <BackLink href="/admin" className="mb-5">
        Bảng điều khiển
      </BackLink>
      <PageHeading
        eyebrow="Tài khoản"
        title="Quản lý người dùng"
        description="Danh sách tài khoản khách hàng, chủ sân, nhân viên và quản trị viên."
      />
      <AdminUsersPanel />
    </>
  );
}
