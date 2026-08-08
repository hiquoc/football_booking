import { BackLink } from "@/components/ui/back-link";
import { ListSkeleton } from "@/components/ui/data-state";
import { PageHeading } from "@/components/ui/page-heading";

export default function AdminPaymentDisputesLoading() {
  return (
    <>
      <BackLink href="/admin" className="mb-5">
        Bảng điều khiển
      </BackLink>
      <PageHeading
        eyebrow="Kiểm duyệt thanh toán"
        title="Tranh chấp thanh toán"
        description="Theo dõi và xử lý các báo cáo tranh chấp do chủ sân gửi lên."
      />
      <div className="mt-8">
        <div className="mb-6 grid gap-3 md:grid-cols-2">
          {[0, 1].map((item) => (
            <div key={item} className="h-20 animate-pulse rounded-2xl border border-slate-200 bg-white" />
          ))}
        </div>
        <ListSkeleton />
      </div>
    </>
  );
}
