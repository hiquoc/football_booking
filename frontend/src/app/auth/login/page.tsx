import type { Metadata } from "next";
import Image from "next/image";
import Link from "next/link";
import { BrandMark } from "@/components/ui/brand-mark";
import { LoginForm } from "@/components/auth/login-form";

export const metadata: Metadata = { title: "Đăng nhập" };

export default async function LoginPage({
  searchParams,
}: {
  searchParams: Promise<{ next?: string }>;
}) {
  const nextPath = (await searchParams).next;
  return (
    <main className="grid min-h-dvh bg-sky-50 lg:grid-cols-[1.35fr_0.65fr]">
      <section className="relative hidden overflow-hidden p-12 text-slate-900 lg:flex lg:flex-col lg:justify-between">
        <div className="absolute inset-0 bg-[linear-gradient(to_right,#80808012_1px,transparent_1px),linear-gradient(to_bottom,#80808012_1px,transparent_1px)] bg-[size:64px_64px] [mask-image:linear-gradient(to_bottom,black,transparent_90%)]" />
        <div className="absolute -left-40 top-1/3 size-[32rem] rounded-full bg-sky-500/25 blur-3xl" />
        <Link
          href="/"
          className="relative z-10 w-fit rounded-2xl bg-white px-4 py-3 shadow-sm"
        >
          <BrandMark />
        </Link>
        <div className="relative z-10 max-w-xl rounded-[2rem] border border-white/60 bg-white/60 p-8 shadow-sm backdrop-blur-md">
          <p className="text-xs font-black uppercase tracking-[0.2em] text-sky-600">
            Đam mê không giới hạn
          </p>
          <h2 className="mt-5 text-6xl font-black leading-[1.1] tracking-[-0.04em]">
            Kết nối đam mê,
            <br />
            Chinh phục sân cỏ.
          </h2>
          <p className="mt-6 max-w-md text-lg leading-8 text-slate-600">
            Tham gia cộng đồng bóng đá lớn nhất, dễ dàng tìm sân, xếp lịch và cháy hết mình trong từng trận đấu.
          </p>
        </div>
        <div className="absolute right-5 top-1/2 z-0 w-[75%] max-w-[550px] -translate-y-1/2">
          <Image
            src="https://res.cloudinary.com/dtvs3rgbw/image/upload/v1782810028/Cristiano_Ronaldo_-_FootyRenders_og72wc.png"
            alt="Cristiano Ronaldo"
            width={700}
            height={900}
            className="h-auto w-full object-contain drop-shadow-[0_20px_30px_rgba(0,0,0,0.15)]"
            priority
          />
        </div>
        <p className="relative z-10 text-xs font-bold uppercase tracking-[0.14em] text-slate-500">
          PitchUp · TP. Hồ Chí Minh
        </p>
      </section>
      <section className="flex min-h-dvh items-center justify-center bg-white px-5 py-12 sm:px-10">
        <div className="w-full max-w-md">
          <Link href="/" className="mb-12 inline-block lg:hidden">
            <BrandMark />
          </Link>
          <LoginForm nextPath={nextPath} />
        </div>
      </section>
    </main>
  );
}
