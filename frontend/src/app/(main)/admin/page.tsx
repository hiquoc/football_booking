import Link from "next/link";
import { ArrowRight, MapPinned, Shapes, Users } from "lucide-react";
import { BackLink } from "@/components/ui/back-link";
import { PageHeading } from "@/components/ui/page-heading";
const cards = [
  {
    href: "/admin/field-types",
    title: "Loại sân",
    description: "Thiết lập môn thể thao và thời lượng mặc định.",
    icon: Shapes,
  },
  {
    href: "/admin/users",
    title: "Người dùng",
    description: "Quản lý tài khoản và phân quyền hệ thống.",
    icon: Users,
  },
  {
    href: "/admin/fields",
    title: "Phê duyệt sân",
    description: "Kiểm tra các địa điểm đang chờ duyệt.",
    icon: MapPinned,
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
      <div className="mt-8 grid gap-4 md:grid-cols-3">
        {cards.map(({ href, title, description, icon: Icon }) => (
          <Link
            key={href}
            href={href}
            className="rounded-[1.5rem] border border-slate-200 bg-white p-6 transition hover:-translate-y-1 hover:border-sky-200 hover:shadow-lg"
          >
            <span className="grid size-11 place-items-center rounded-xl bg-sky-100 text-sky-700">
              <Icon className="size-5" />
            </span>
            <h2 className="mt-5 text-xl font-black">{title}</h2>
            <p className="mt-2 text-sm leading-6 text-slate-500">
              {description}
            </p>
            <span className="mt-5 inline-flex items-center gap-2 text-sm font-black text-sky-600">
              Mở quản lý <ArrowRight className="size-4" />
            </span>
          </Link>
        ))}
      </div>
    </>
  );
}
