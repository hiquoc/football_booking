import { Suspense, type ReactNode } from "react";
import Image from "next/image";
import Link from "next/link";
import {
  ArrowDown,
  CalendarDays,
  CheckCircle2,
  Clock3,
  CreditCard,
  MapPin,
  Search,
  ShieldCheck,
  Sparkles,
  Star,
  UsersRound,
} from "lucide-react";
import { FeaturedFields } from "@/components/fields/featured-fields";
import { FieldCard } from "@/components/fields/field-card";
import { LandingFieldSearch } from "@/components/fields/landing-field-search";
import { fieldTypeOptions } from "@/lib/field-format";
import { getRecentlyBookedFieldCards } from "@/lib/server/fields";
import { getCurrentUser } from "@/lib/server/session";

export default function HomePage() {
  return (
    <>
      <section className="relative isolate overflow-hidden bg-white text-slate-950">
        <Image
          src="https://res.cloudinary.com/dtvs3rgbw/image/upload/v1782712257/Untitled_design_tsqdxg.png"
          alt="Cầu thủ bóng đá chuẩn bị cho trận đấu"
          width={1200}
          height={900}
          priority
          className="pointer-events-none absolute bottom-0 right-[-7rem] top-16 -z-10 hidden h-[88%] w-auto max-w-none object-contain opacity-95 drop-shadow-[0_30px_55px_rgba(15,23,42,0.18)] lg:block xl:right-[-3rem]"
        />
        <div className="absolute inset-0 -z-20 bg-[linear-gradient(to_bottom,#f0fdf4_0%,#ffffff_46%,#f8fafc_100%)]" />
        <div className="absolute inset-0 -z-10 bg-[linear-gradient(to_right,rgba(34,197,94,0.08)_1px,transparent_1px),linear-gradient(to_bottom,rgba(34,197,94,0.08)_1px,transparent_1px)] bg-[size:72px_72px] [mask-image:linear-gradient(to_bottom,black,transparent_82%)]" />

        <div className="mx-auto flex min-h-[720px] w-full max-w-[90rem] items-center px-5 py-16 sm:px-8 lg:py-20">
          <div className="w-full max-w-3xl">
            <span className="inline-flex items-center gap-2 rounded-full border border-green-200 bg-white px-4 py-2 text-sm font-extrabold uppercase text-green-700 shadow-sm">
              <Sparkles className="size-4" aria-hidden="true" />
              Nền tảng đặt sân thể thao tại Việt Nam
            </span>
            <h1 className="mt-7 max-w-3xl text-5xl font-black leading-tight text-slate-950 sm:text-6xl lg:text-[56px]">
              <span className="block">Đặt sân thể thao</span>
              <span className="block">nhanh chóng & tiện lợi</span>
            </h1>
            <p className="mt-6 max-w-2xl text-lg leading-8 text-slate-600">
              <span className="block">Khám phá sân chất lượng quanh bạn, xem lịch trống và đặt khung giờ phù hợp mà không cần gọi điện.</span>
              <span className="block"></span>
            </p>

            <form
              action="/fields"
              className="mt-9 rounded-2xl border border-slate-200 bg-white p-3 shadow-[0_18px_50px_rgba(15,23,42,0.10)]"
            >
              <div className="grid gap-3 lg:grid-cols-[1.25fr_0.75fr_auto]">
                <label className="flex min-h-14 items-center gap-3 rounded-xl border border-slate-200 bg-slate-50 px-4 text-base font-bold text-slate-700 focus-within:border-green-500 focus-within:bg-white focus-within:ring-4 focus-within:ring-green-100">
                  <Search className="size-5 shrink-0 text-green-600" aria-hidden="true" />
                  <span className="sr-only">Tên sân</span>
                  <input
                    name="keyword"
                    type="search"
                    placeholder="Nhập tên sân hoặc khu vực"
                    className="min-w-0 flex-1 bg-transparent py-3 text-slate-950 outline-none placeholder:text-slate-400"
                  />
                </label>
                <label className="flex min-h-14 items-center gap-3 rounded-xl border border-slate-200 bg-slate-50 px-4 text-base font-bold text-slate-700 focus-within:border-green-500 focus-within:bg-white focus-within:ring-4 focus-within:ring-green-100">
                  <MapPin className="size-5 shrink-0 text-green-600" aria-hidden="true" />
                  <span className="sr-only">Môn thể thao</span>
                  <select
                    name="fieldType"
                    defaultValue="FOOTBALL"
                    className="min-w-0 flex-1 bg-transparent py-3 text-slate-950 outline-none"
                  >
                    <option value="">Tất cả môn thể thao</option>
                    {fieldTypeOptions.map(([value, label]) => (
                      <option key={value} value={value}>
                        {label}
                      </option>
                    ))}
                  </select>
                </label>
                <button className="inline-flex min-h-14 items-center justify-center gap-2 rounded-xl bg-green-500 px-6 text-base font-black text-white shadow-[0_12px_26px_rgba(34,197,94,0.24)] hover:bg-green-600">
                  Tìm sân
                  <ArrowDown className="size-5" aria-hidden="true" />
                </button>
              </div>
            </form>

            <div className="mt-8 flex flex-wrap gap-3">
              <a
                href="#fields"
                className="inline-flex min-h-12 items-center gap-2 rounded-xl border border-green-500 bg-white px-5 text-base font-black text-green-700 hover:bg-green-50"
              >
                Khám phá sân nổi bật
                <ArrowDown className="size-5" aria-hidden="true" />
              </a>
              <Link
                href="/community"
                className="inline-flex min-h-12 items-center gap-2 rounded-xl border border-slate-200 bg-white px-5 text-base font-black text-slate-700 hover:border-green-300 hover:text-green-700"
              >
                Tìm đội chơi
                <UsersRound className="size-5" aria-hidden="true" />
              </Link>
            </div>
          </div>
        </div>
      </section>

      <section className="bg-white py-16 sm:py-20">
        <div className="mx-auto grid w-full max-w-[90rem] gap-4 px-5 sm:px-8 md:grid-cols-3">
          <StatCard value="6+" label="Sân nổi bật được đề xuất" />
          <StatCard value="60s" label="Dữ liệu sân được làm mới định kỳ" />
          <StatCard value="24/7" label="Tra cứu thông tin trước khi đặt" />
        </div>
      </section>

      <Suspense fallback={null}>
        <RecentlyBookedFields />
      </Suspense>

      <section id="fields" className="scroll-mt-24 bg-slate-50 py-20 sm:py-28">
        <div className="mx-auto w-full max-w-[90rem] px-5 sm:px-8">
          <div className="mb-8">
            <div>
              <p className="text-sm font-black uppercase text-green-600">
                Tìm sân
              </p>
              <h2 className="mt-3 text-4xl font-black leading-tight text-slate-950 sm:text-5xl">
                Tìm sân phù hợp với lịch của bạn.
              </h2>
            </div>
            <div className="mt-6">
              <LandingFieldSearch />
            </div>
          </div>
          <div className="mb-10">
            <p className="text-sm font-black uppercase text-green-600">
              Được đánh giá cao
            </p>
            <h3 className="mt-3 text-3xl font-black leading-tight text-slate-950 sm:text-4xl">
              Sân nổi bật gần đây.
            </h3>
          </div>
          <Suspense fallback={<FieldsSkeleton />}>
            <FeaturedFields />
          </Suspense>
        </div>
      </section>

      <section className="bg-white py-20 sm:py-28">
        <div className="mx-auto w-full max-w-[90rem] px-5 sm:px-8">
          <div className="max-w-3xl">
            <p className="text-sm font-black uppercase text-green-600">
              Vì sao dễ sử dụng
            </p>
            <h2 className="mt-3 text-4xl font-black leading-tight text-slate-950 sm:text-5xl">
              Mọi thao tác rõ ràng từ lúc tìm sân đến khi ra sân.
            </h2>
          </div>
          <div className="mt-10 grid gap-5 md:grid-cols-2 xl:grid-cols-4">
            <ValueCard
              icon={<CalendarDays />}
              title="Đặt sân nhanh"
              copy="Chọn sân, xem thông tin cần thiết và tiếp tục tới lịch đặt chỉ trong vài bước."
            />
            <ValueCard
              icon={<CreditCard />}
              title="Thanh toán rõ ràng"
              copy="Quy trình thanh toán nằm trong luồng đặt sân hiện có, dễ theo dõi trên mọi thiết bị."
            />
            <ValueCard
              icon={<ShieldCheck />}
              title="Chủ sân xác minh"
              copy="Thông tin sân, địa chỉ và đánh giá giúp người chơi tự tin hơn trước khi chọn."
            />
            <ValueCard
              icon={<UsersRound />}
              title="Tìm trận phù hợp"
              copy="Khu vực cộng đồng giúp người chơi kết nối và tìm đội khi cần thêm người."
            />
          </div>
        </div>
      </section>

      <section className="bg-green-50 py-20 sm:py-28">
        <div className="mx-auto grid w-full max-w-[90rem] gap-8 px-5 sm:px-8 lg:grid-cols-[0.8fr_1.2fr] lg:items-center">
          <div>
            <p className="text-sm font-black uppercase text-green-700">
              Trải nghiệm thân thiện
            </p>
            <h2 className="mt-3 text-4xl font-black leading-tight text-slate-950 sm:text-5xl">
              Thiết kế dành cho người muốn đặt sân thật nhanh.
            </h2>
            <p className="mt-5 text-lg leading-8 text-slate-600">
              Nút lớn, chữ dễ đọc và khoảng cách thoáng giúp người dùng mới vẫn
              hiểu ngay bước tiếp theo.
            </p>
          </div>
          <div className="grid gap-4 sm:grid-cols-3">
            <TrustCard icon={<Clock3 />} title="Tiết kiệm thời gian" />
            <TrustCard icon={<Star />} title="Ưu tiên sân uy tín" />
            <TrustCard icon={<CheckCircle2 />} title="Giữ đủ thao tác cũ" />
          </div>
        </div>
      </section>
    </>
  );
}

async function RecentlyBookedFields() {
  const user = await getCurrentUser();
  if (!user || (user.userType !== "CLIENT" && user.userType !== "EMPLOYEE")) {
    return null;
  }

  const fields = await getRecentlyBookedFieldCards(user.id, 4).catch(() => []);
  if (!fields.length) return null;

  return (
    <section className="bg-white py-20 sm:py-24">
      <div className="mx-auto w-full max-w-[90rem] px-5 sm:px-8">
        <div className="mb-10 flex flex-col justify-between gap-4 md:flex-row md:items-end">
          <div>
            <p className="text-sm font-black uppercase text-green-600">
              Quay lại sân quen
            </p>
            <h2 className="mt-3 text-4xl font-black leading-tight text-slate-950 sm:text-5xl">
              Những sân bạn đã đặt
            </h2>
          </div>
          <Link
            href="/bookings"
            className="inline-flex min-h-12 items-center justify-center rounded-xl border border-green-500 bg-white px-5 text-base font-black text-green-700 hover:bg-green-50"
          >
            Xem lịch đặt
          </Link>
        </div>
        <div className="grid gap-6 md:grid-cols-2 xl:grid-cols-4">
          {fields.map((field) => (
            <FieldCard key={field.id} field={field} canFavorite />
          ))}
        </div>
      </div>
    </section>
  );
}

function StatCard({ value, label }: { value: string; label: string }) {
  return (
    <article className="rounded-2xl border border-slate-200 bg-white p-6 shadow-[0_12px_32px_rgba(15,23,42,0.05)]">
      <p className="text-4xl font-black text-green-600">{value}</p>
      <p className="mt-2 text-base font-bold leading-7 text-slate-600">{label}</p>
    </article>
  );
}

function ValueCard({
  icon,
  title,
  copy,
}: {
  icon: ReactNode;
  title: string;
  copy: string;
}) {
  return (
    <article className="rounded-2xl border border-slate-200 bg-white p-7 text-slate-900 shadow-[0_12px_32px_rgba(15,23,42,0.05)] transition hover:-translate-y-1 hover:border-green-300 hover:shadow-[0_18px_40px_rgba(15,23,42,0.09)]">
      <span className="grid size-12 place-items-center rounded-xl bg-green-100 text-green-700 [&_svg]:size-6">
        {icon}
      </span>
      <h3 className="mt-6 text-xl font-black text-slate-950">{title}</h3>
      <p className="mt-3 text-base leading-7 text-slate-600">{copy}</p>
    </article>
  );
}

function TrustCard({ icon, title }: { icon: ReactNode; title: string }) {
  return (
    <article className="rounded-2xl border border-green-200 bg-white p-6 shadow-[0_12px_32px_rgba(21,128,61,0.08)]">
      <span className="grid size-12 place-items-center rounded-xl bg-green-500 text-white [&_svg]:size-6">
        {icon}
      </span>
      <h3 className="mt-5 text-xl font-black leading-7 text-slate-950">{title}</h3>
    </article>
  );
}

function FieldsSkeleton() {
  return (
    <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-3">
      {[0, 1, 2].map((item) => (
        <div
          key={item}
          className="h-96 animate-pulse rounded-2xl bg-slate-200"
        />
      ))}
    </div>
  );
}
