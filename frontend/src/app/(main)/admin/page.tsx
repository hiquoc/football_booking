import Link from "next/link";
import { ArrowRight, Gavel, MapPinned, Shapes, ShieldAlert, Users } from "lucide-react";
import { BackLink } from "@/components/ui/back-link";
import { PageHeading } from "@/components/ui/page-heading";

const cards = [
  {
    href: "/admin/field-types",
    title: "Loại sân",
    description: "Thiết lập môn thể thao và thời lượng đặt sân mặc định.",
    icon: Shapes,
  },
  {
    href: "/admin/users",
    title: "Người dùng",
    description: "Quản lý tài khoản, vai trò và trạng thái sử dụng hệ thống.",
    icon: Users,
  },
  {
    href: "/admin/fields",
    title: "Phê duyệt sân",
    description: "Kiểm tra các địa điểm đang chờ duyệt trước khi hiển thị công khai.",
    icon: MapPinned,
  },
  {
    href: "/admin/community-moderation",
    title: "Kiểm duyệt cộng đồng",
    description: "Xử lý báo cáo, ẩn bài và quản lý vi phạm đăng bài.",
    icon: ShieldAlert,
  },
  {
    href: "/admin/payment-disputes",
    title: "Tranh chấp thanh toán",
    description: "Lọc, xem chi tiết và cấm hoặc bỏ cấm người chơi từ danh sách tranh chấp.",
    icon: Gavel,
  },
];

export default function AdminPage() {
  return (
    <>
      <BackLink href="/" className="mb-5">
        Trang chủ
      </BackLink>
      <PageHeading
        eyebrow="Quản trị hệ thống"
        title="Bảng điều khiển"
        description="Cấu hình danh mục và kiểm soát hoạt động của nền tảng."
      />
      <div className="mt-8 grid gap-4 md:grid-cols-2 xl:grid-cols-3">
        {cards.map(({ href, title, description, icon: Icon }) => (
          <Link
            key={href}
            href={href}
            className="group rounded-2xl border border-slate-200 bg-white p-6 shadow-sm transition hover:-translate-y-1 hover:border-green-300 hover:shadow-[0_18px_40px_rgba(15,23,42,0.08)]"
          >
            <span className="grid size-11 place-items-center rounded-xl bg-green-50 text-green-700">
              <Icon className="size-5" />
            </span>
            <h2 className="mt-5 text-xl font-black text-slate-950">{title}</h2>
            <p className="mt-2 text-sm leading-6 text-slate-500">
              {description}
            </p>
            <span className="mt-5 inline-flex items-center gap-2 text-sm font-black text-green-700">
              Mở quản lý <ArrowRight className="size-4 transition group-hover:translate-x-1" />
            </span>
          </Link>
        ))}
      </div>
    </>
  );
}
