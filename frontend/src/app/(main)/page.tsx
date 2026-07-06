import { Suspense } from "react";
import Image from "next/image";
import {
  ArrowDown,
  CalendarDays,
  CheckCircle2,
  Search,
  ShieldCheck,
  Sparkles,
} from "lucide-react";
import { FeaturedFields } from "@/components/fields/featured-fields";

export default function HomePage() {
  return (
    <>
      <section className="relative isolate overflow-hidden bg-sky-50 text-slate-900">
        <div className="absolute inset-0 -z-10 bg-[linear-gradient(to_right,#80808012_1px,transparent_1px),linear-gradient(to_bottom,#80808012_1px,transparent_1px)] bg-[size:64px_64px] [mask-image:linear-gradient(to_bottom,black,transparent_90%)]" />
        <div className="absolute -right-36 -top-36 -z-10 size-[34rem] rounded-full bg-sky-300/30 blur-3xl" />
        <div className="absolute -bottom-64 left-1/3 -z-10 size-[30rem] rounded-full bg-indigo-300/20 blur-3xl" />
        <div className="mx-auto grid min-h-[650px] w-full max-w-[90rem] items-center gap-14 px-5 py-20 sm:px-8 lg:grid-cols-[1.1fr_0.9fr] lg:gap-20 lg:py-24 xl:gap-28">
          <div>
            <span className="inline-flex items-center gap-2 rounded-full border border-sky-200 bg-sky-100 px-4 py-2 text-xs font-black uppercase tracking-[0.16em] text-sky-700">
              <Sparkles className="size-4" aria-hidden="true" /> Nền tảng đặt
              sân thể thao tại Việt Nam
            </span>
            <h1 className="mt-7 max-w-4xl text-5xl font-black leading-[1.15] tracking-[-0.065em] sm:text-6xl lg:text-7xl">
              Trận đấu tiếp theo
              <br />
              <span className="text-sky-600">bắt đầu tại đây.</span>
            </h1>
            <p className="mt-7 max-w-xl text-base leading-7 text-slate-600 sm:text-lg">
              Khám phá sân chất lượng, xem lịch trống và giữ khung giờ phù hợp
              mà không cần gọi điện.
            </p>
            <div className="mt-9 flex flex-wrap gap-3">
              <a
                href="#fields"
                className="inline-flex items-center gap-2 rounded-full bg-sky-500 px-6 py-3.5 text-sm font-black text-white transition hover:bg-sky-400"
              >
                Khám phá sân <ArrowDown className="size-4" aria-hidden="true" />
              </a>
            </div>
          </div>
          <div className="relative mx-auto hidden w-full max-w-2xl lg:-translate-x-8 lg:block lg:max-w-none lg:w-[120%] xl:-translate-x-16 xl:w-[140%] 2xl:-translate-x-20 2xl:w-[150%]">
            <div className="absolute inset-10 -z-10 rounded-full bg-sky-300/30 blur-3xl" />
            <Image
              src="https://res.cloudinary.com/dtvs3rgbw/image/upload/v1782712257/Untitled_design_tsqdxg.png"
              alt="Cầu thủ bóng đá chuẩn bị cho trận đấu"
              width={900}
              height={900}
              priority
              className="h-auto w-full object-contain drop-shadow-[0_28px_45px_rgba(0,0,0,0.15)]"
            />
          </div>
        </div>
      </section>

      <section id="fields" className="scroll-mt-24 py-20 sm:py-28">
        <div className="mx-auto w-full max-w-[90rem] px-5 sm:px-8">
          <div className="mb-10 flex flex-col justify-between gap-5 md:flex-row md:items-end">
            <div>
              <p className="text-xs font-black uppercase tracking-[0.18em] text-sky-600">
                Được đánh giá cao
              </p>
              <h2 className="mt-3 text-3xl font-black tracking-[-0.045em] text-slate-950 sm:text-5xl">
                Tìm sân quen của bạn.
              </h2>
            </div>
            <div className="inline-flex max-w-md items-center gap-3 rounded-2xl border border-slate-200 bg-white px-4 py-3 text-sm text-slate-400 shadow-sm">
              <Search className="size-5 text-sky-500" aria-hidden="true" />
              Tìm kiếm và bộ lọc sẽ sớm được cập nhật.
            </div>
          </div>
          <Suspense fallback={<FieldsSkeleton />}>
            <FeaturedFields />
          </Suspense>
        </div>
      </section>

      <section className="pb-20 sm:pb-28 bg-slate-50">
        <div className="mx-auto grid w-full max-w-[90rem] gap-4 px-5 sm:px-8 md:grid-cols-3">
          <ValueCard
            icon={<ShieldCheck />}
            number="01"
            title="Địa điểm đã xác minh"
            copy="Thông tin rõ ràng và đánh giá đáng tin cậy từ người chơi."
          />
          <ValueCard
            icon={<CalendarDays />}
            number="02"
            title="Lịch trống trực tiếp"
            copy="Biết trước khung giờ phù hợp để dễ dàng sắp xếp đội hình."
          />
          <ValueCard
            icon={<CheckCircle2 />}
            number="03"
            title="Đặt sân đơn giản"
            copy="Chọn sân, xác nhận thời gian và sẵn sàng thi đấu."
          />
        </div>
      </section>
    </>
  );
}

function ValueCard({
  icon,
  number,
  title,
  copy,
}: {
  icon: React.ReactNode;
  number: string;
  title: string;
  copy: string;
}) {
  return (
    <article className="relative overflow-hidden rounded-[1.75rem] border border-sky-100 bg-white p-7 text-slate-900 shadow-[0_12px_40px_rgba(15,23,42,0.04)]">
      <span className="absolute right-5 top-3 text-6xl font-black text-sky-900/[0.04]">
        {number}
      </span>
      <span className="grid size-11 place-items-center rounded-xl bg-sky-100 text-sky-700 [&_svg]:size-5">
        {icon}
      </span>
      <h3 className="mt-6 text-lg font-black">{title}</h3>
      <p className="mt-2 text-sm leading-6 text-slate-500">{copy}</p>
    </article>
  );
}

function FieldsSkeleton() {
  return (
    <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-3">
      {[0, 1, 2].map((item) => (
        <div
          key={item}
          className="h-96 animate-pulse rounded-[1.75rem] bg-slate-200"
        />
      ))}
    </div>
  );
}
