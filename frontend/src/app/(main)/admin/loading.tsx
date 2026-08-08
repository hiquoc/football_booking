import { PageHeading } from "@/components/ui/page-heading";

export default function AdminLoading() {
  return (
    <>
      <PageHeading
        eyebrow="Quản trị hệ thống"
        title="Bảng điều khiển"
        description="Cấu hình danh mục và kiểm soát hoạt động của nền tảng."
      />
      <div className="mt-8 grid gap-4 md:grid-cols-2 xl:grid-cols-3">
        {[0, 1, 2, 3, 4].map((item) => (
          <div key={item} className="h-56 animate-pulse rounded-2xl border border-slate-200 bg-white" />
        ))}
      </div>
    </>
  );
}
