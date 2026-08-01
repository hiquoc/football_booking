import { BackLink } from "@/components/ui/back-link";
import { PageHeading } from "@/components/ui/page-heading";
import { NotificationList } from "@/components/notifications/notification-list";

export default function NotificationsPage() {
  return (
    <div className="mx-auto w-full max-w-6xl px-4 py-8 sm:px-6 lg:px-8">
      <BackLink href="/" className="mb-5">
        Trang chủ
      </BackLink>
      <PageHeading
        eyebrow="Thông báo"
        title="Thông báo của bạn"
        description="Theo dõi cập nhật mới nhất về đặt sân, thanh toán và hoạt động cộng đồng."
      />
      <div className="mt-6">
        <NotificationList />
      </div>
    </div>
  );
}
