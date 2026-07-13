import type { Metadata } from "next";
import Link from "next/link";
import {
  Building2,
  CalendarCheck2,
  CheckCircle2,
  Mail,
  MapPinned,
  MessageCircle,
  Phone,
  ShieldCheck,
  UsersRound,
} from "lucide-react";

export const metadata: Metadata = {
  title: "Liên hệ",
  description:
    "Tìm hiểu PitchUp và liên hệ hợp tác dịch vụ đặt sân thể thao cho chủ sân.",
};

const contactChannels = [
  {
    icon: Mail,
    title: "Email hợp tác",
    value: "partners@pitchup.vn",
    href: "mailto:partners@pitchup.vn",
    note: "Gửi thông tin sân, khu vực hoạt động và nhu cầu quản lý lịch.",
  },
  {
    icon: Phone,
    title: "Điện thoại",
    value: "+84 900 123 456",
    href: "tel:+84900123456",
    note: "Phù hợp khi chủ sân cần trao đổi nhanh về quy trình đăng ký.",
  },
  {
    icon: MessageCircle,
    title: "Tư vấn vận hành",
    value: "support@pitchup.vn",
    href: "mailto:support@pitchup.vn",
    note: "Hỗ trợ cập nhật thông tin sân, hình ảnh, khung giờ và đơn đặt.",
  },
];

const ownerSteps = [
  "Chia sẻ thông tin sân, địa chỉ, số lượng sân con và loại mặt sân.",
  "Cung cấp khung giờ hoạt động, giá thuê và quy định đặt cọc nếu có.",
  "Xác minh quyền sở hữu hoặc quản lý để PitchUp kích hoạt khu vực chủ sân.",
];

export default function ContactPage() {
  return (
    <div className="min-h-[70vh] bg-slate-50">
      <section className="relative overflow-hidden bg-sky-50 py-16 text-slate-900 sm:py-20">
        <div className="absolute inset-0 bg-[linear-gradient(to_right,#80808012_1px,transparent_1px),linear-gradient(to_bottom,#80808012_1px,transparent_1px)] bg-[size:64px_64px] [mask-image:linear-gradient(to_bottom,black,transparent_90%)]" />
        <div className="relative mx-auto grid w-full max-w-[90rem] gap-10 px-5 sm:px-8 lg:grid-cols-[1fr_0.8fr] lg:items-end">
          <div>
            <span className="inline-flex items-center gap-2 text-xs font-black uppercase tracking-[0.18em] text-sky-600">
              <Mail className="size-4" aria-hidden="true" /> Liên hệ PitchUp
            </span>
            <h1 className="mt-4 max-w-4xl text-4xl font-black tracking-[-0.05em] text-slate-950 sm:text-6xl">
              Kết nối người chơi với những sân thể thao phù hợp.
            </h1>
            <p className="mt-5 max-w-2xl leading-7 text-slate-600">
              PitchUp giúp người chơi tìm sân, xem thông tin, kiểm tra lịch
              trống và đặt sân trực tuyến. Chủ sân có thể hợp tác để đưa địa
              điểm lên nền tảng, quản lý sản phẩm sân và tiếp cận thêm khách
              hàng.
            </p>
          </div>
          <div className="rounded-[1.75rem] border border-sky-100 bg-white p-6 shadow-[0_12px_40px_rgba(15,23,42,0.05)]">
            <p className="text-xs font-black uppercase tracking-[0.16em] text-sky-600">
              Dành cho chủ sân
            </p>
            <h2 className="mt-3 text-2xl font-black tracking-[-0.035em] text-slate-950">
              Muốn đưa sân lên PitchUp?
            </h2>
            <p className="mt-3 text-sm leading-6 text-slate-500">
              Gửi thông tin qua email hoặc điện thoại. Đội ngũ PitchUp sẽ liên
              hệ để xác minh, hỗ trợ nhập dữ liệu và hướng dẫn vận hành.
            </p>
            <Link
              href="mailto:partners@pitchup.vn"
              className="mt-6 inline-flex items-center gap-2 rounded-full bg-sky-500 px-5 py-3 text-sm font-black text-white hover:bg-sky-600"
            >
              <Mail className="size-4" aria-hidden="true" /> Gửi email hợp tác
            </Link>
          </div>
        </div>
      </section>

      <section className="mx-auto grid w-full max-w-[90rem] gap-6 px-5 py-12 sm:px-8 sm:py-16 lg:grid-cols-3">
        {contactChannels.map(({ icon: Icon, title, value, href, note }) => (
          <article
            key={title}
            className="rounded-[1.75rem] border border-slate-200 bg-white p-7 shadow-[0_12px_40px_rgba(15,23,42,0.04)]"
          >
            <span className="grid size-11 place-items-center rounded-xl bg-sky-100 text-sky-700">
              <Icon className="size-5" aria-hidden="true" />
            </span>
            <h2 className="mt-6 text-lg font-black text-slate-950">{title}</h2>
            <Link href={href} className="mt-2 inline-flex text-sm font-black text-sky-600 hover:text-sky-700">
              {value}
            </Link>
            <p className="mt-3 text-sm leading-6 text-slate-500">{note}</p>
          </article>
        ))}
      </section>

      <section className="bg-white py-12 sm:py-16">
        <div className="mx-auto grid w-full max-w-[90rem] gap-10 px-5 sm:px-8 lg:grid-cols-[0.85fr_1fr] lg:items-start">
          <div>
            <p className="text-xs font-black uppercase tracking-[0.18em] text-sky-600">
              PitchUp làm gì?
            </p>
            <h2 className="mt-3 text-3xl font-black tracking-[-0.045em] text-slate-950 sm:text-5xl">
              Một nền tảng cho cả người chơi và chủ sân.
            </h2>
            <p className="mt-5 leading-7 text-slate-600">
              Người chơi có thể khám phá sân, xem địa điểm, đánh giá, giá tham
              khảo và khung giờ phù hợp. Chủ sân có khu vực quản lý để cập nhật
              thông tin sân, sân con, hình ảnh, lịch đóng/mở và theo dõi đơn
              đặt.
            </p>
          </div>
          <div className="grid gap-4 sm:grid-cols-2">
            <InfoTile
              icon={<MapPinned />}
              title="Hiển thị sân"
              copy="Giúp khách hàng tìm thấy sân theo vị trí, loại sân và nhu cầu thi đấu."
            />
            <InfoTile
              icon={<CalendarCheck2 />}
              title="Quản lý lịch"
              copy="Hỗ trợ xem khung giờ, tạo đơn đặt và theo dõi trạng thái booking."
            />
            <InfoTile
              icon={<ShieldCheck />}
              title="Thông tin rõ ràng"
              copy="Sân có trang chi tiết với hình ảnh, quy định, giá và đánh giá của người chơi."
            />
            <InfoTile
              icon={<UsersRound />}
              title="Hợp tác lâu dài"
              copy="PitchUp hỗ trợ chủ sân chuẩn hóa dữ liệu và vận hành kênh đặt sân online."
            />
          </div>
        </div>
      </section>

      <section className="mx-auto w-full max-w-[90rem] px-5 py-12 sm:px-8 sm:py-16">
        <div className="grid gap-8 rounded-[1.75rem] border border-slate-200 bg-slate-950 p-7 text-white sm:p-9 lg:grid-cols-[0.75fr_1fr]">
          <div>
            <span className="inline-flex items-center gap-2 text-xs font-black uppercase tracking-[0.18em] text-sky-300">
              <Building2 className="size-4" aria-hidden="true" /> Quy trình hợp tác
            </span>
            <h2 className="mt-4 text-3xl font-black tracking-[-0.045em] sm:text-4xl">
              Chủ sân cần chuẩn bị gì?
            </h2>
          </div>
          <div className="grid gap-4">
            {ownerSteps.map((step) => (
              <div key={step} className="flex gap-3 rounded-2xl bg-white/8 p-4">
                <CheckCircle2 className="mt-0.5 size-5 shrink-0 text-sky-300" aria-hidden="true" />
                <p className="text-sm leading-6 text-slate-200">{step}</p>
              </div>
            ))}
          </div>
        </div>
      </section>
    </div>
  );
}

function InfoTile({
  icon,
  title,
  copy,
}: {
  icon: React.ReactNode;
  title: string;
  copy: string;
}) {
  return (
    <article className="rounded-[1.5rem] border border-slate-200 bg-slate-50 p-6">
      <span className="grid size-10 place-items-center rounded-xl bg-white text-sky-700 shadow-sm [&_svg]:size-5">
        {icon}
      </span>
      <h3 className="mt-5 text-base font-black text-slate-950">{title}</h3>
      <p className="mt-2 text-sm leading-6 text-slate-500">{copy}</p>
    </article>
  );
}
