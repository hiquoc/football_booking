import { PageHeading } from "@/components/ui/page-heading";

export default function AdminCommunityModerationLoading() {
  return (
    <>
      <PageHeading
        eyebrow="Kiểm duyệt"
        title="Kiểm duyệt cộng đồng"
        description="Xem báo cáo, ẩn bài, cảnh báo và cấm đăng trong khu vực cộng đồng."
      />
      <div className="mt-8 space-y-4">
        {[0, 1, 2].map((item) => (
          <div key={item} className="h-44 animate-pulse rounded-2xl border border-slate-200 bg-white" />
        ))}
      </div>
    </>
  );
}
