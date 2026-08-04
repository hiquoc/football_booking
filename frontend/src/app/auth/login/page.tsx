import type { Metadata } from "next";
import Image from "next/image";
import Link from "next/link";
import { BrandMark } from "@/components/ui/brand-mark";
import { LoginForm } from "@/components/auth/login-form";
import {
  AUTH_REDIRECT_PARAM,
  safeAuthRedirect,
} from "@/lib/auth-redirect";

export const metadata: Metadata = { title: "Đăng nhập" };

export default async function LoginPage({
  searchParams,
}: {
  searchParams: Promise<{ next?: string; redirect?: string }>;
}) {
  const params = await searchParams;
  const nextPath = safeAuthRedirect(
    params[AUTH_REDIRECT_PARAM] ?? params.next,
  );
  return (
    <main className="grid min-h-dvh bg-slate-50 lg:grid-cols-[1.25fr_0.75fr]">
      <section className="relative hidden overflow-hidden bg-[linear-gradient(to_bottom,#f0fdf4_0%,#ffffff_52%,#f8fafc_100%)] p-12 text-slate-900 lg:flex lg:flex-col lg:justify-between">
        <div className="absolute inset-0 bg-[linear-gradient(to_right,rgba(34,197,94,0.08)_1px,transparent_1px),linear-gradient(to_bottom,rgba(34,197,94,0.08)_1px,transparent_1px)] bg-[size:72px_72px] [mask-image:linear-gradient(to_bottom,black,transparent_86%)]" />
        <Link
          href="/"
          className="relative z-10 w-fit rounded-2xl border border-slate-200 bg-white px-4 py-3 shadow-sm"
        >
          <BrandMark />
        </Link>
        <div className="relative z-10 max-w-xl">
          <p className="text-sm font-black uppercase text-green-600">
            Đam mê không giới hạn
          </p>
          <h2 className="mt-5 text-6xl font-black leading-[1.1]">
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
      <section className="flex min-h-dvh items-center justify-center border-l border-slate-200 bg-white px-5 py-12 sm:px-10">
        <div className="w-full max-w-md rounded-2xl border border-slate-200 bg-white p-6 shadow-[0_18px_45px_rgba(15,23,42,0.08)] sm:p-8">
          <Link href="/" className="mb-12 inline-block lg:hidden">
            <BrandMark />
          </Link>
          <LoginForm nextPath={nextPath} />
        </div>
      </section>
    </main>
  );
}
