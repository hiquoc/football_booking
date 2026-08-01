import { BackLink } from "@/components/ui/back-link";
import { PageHeading } from "@/components/ui/page-heading";

export default function SupportPage() {
  const email = process.env.NEXT_PUBLIC_SUPPORT_EMAIL ?? "support@example.com";
  const phone = process.env.NEXT_PUBLIC_SUPPORT_PHONE ?? "+84 000 000 000";

  return (
    <section className="mx-auto w-full max-w-4xl px-4 py-8 sm:px-6 lg:px-8">
      <BackLink href="/" className="mb-5">
        Trang chủ
      </BackLink>
      <PageHeading
        eyebrow="Hỗ trợ"
        title="Thông tin kháng nghị"
        description="Tài khoản có thể bị hạn chế sau nhiều lần bị cấm tại sân hoặc khi tranh chấp thanh toán được chấp nhận."
      />
      <div className="mt-6 rounded-2xl border border-slate-200 bg-white p-6 text-sm leading-6 text-slate-600 shadow-sm">
        <p>
          Khi gửi kháng nghị, hãy cung cấp số điện thoại, mã đặt sân và bằng chứng liên quan để đội ngũ hỗ trợ kiểm tra nhanh hơn.
        </p>
        <div className="mt-5 grid gap-3 sm:grid-cols-2">
          <p className="rounded-xl bg-slate-50 p-4 font-semibold text-slate-900">
            Email: {email}
          </p>
          <p className="rounded-xl bg-slate-50 p-4 font-semibold text-slate-900">
            Điện thoại: {phone}
          </p>
        </div>
      </div>
    </section>
  );
}
