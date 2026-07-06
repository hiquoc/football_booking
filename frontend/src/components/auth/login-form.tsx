"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import {
  ArrowLeft,
  ArrowRight,
  LoaderCircle,
  ShieldCheck,
} from "lucide-react";
import { useState } from "react";
import { useForm } from "react-hook-form";
import {
  sendOtpSchema,
  verifyOtpSchema,
  type SendOtpInput,
  type VerifyOtpInput,
} from "@/lib/api/auth-schemas";
import { useSendOtp, useVerifyOtp } from "@/lib/hooks/use-auth";

type Step = "phone" | "otp";

export function LoginForm({ nextPath = "/" }: { nextPath?: string }) {
  const sendOtpMutation = useSendOtp();
  const verifyOtpMutation = useVerifyOtp();
  const [step, setStep] = useState<Step>("phone");
  const [phoneNumber, setPhoneNumber] = useState("");

  const phoneForm = useForm<SendOtpInput>({
    resolver: zodResolver(sendOtpSchema),
    defaultValues: { phoneNumber: "" },
  });
  const otpForm = useForm<VerifyOtpInput>({
    resolver: zodResolver(verifyOtpSchema),
    defaultValues: { phoneNumber: "", code: "" },
  });

  async function sendOtp(input: SendOtpInput) {
    try {
      await sendOtpMutation.mutateAsync(input);
      setPhoneNumber(input.phoneNumber);
      otpForm.setValue("phoneNumber", input.phoneNumber);
      setStep("otp");
    } catch {
      // React Query stores the error for display below.
    }
  }

  async function verifyOtp(input: VerifyOtpInput) {
    try {
      await verifyOtpMutation.mutateAsync(input);
      window.location.replace(
        nextPath.startsWith("/") && !nextPath.startsWith("//") ? nextPath : "/",
      );
    } catch {
      // React Query stores the error for display below.
    }
  }

  if (step === "otp") {
    return (
      <form
        onSubmit={otpForm.handleSubmit(verifyOtp)}
        className="space-y-5"
        noValidate
      >
        <button
          type="button"
          onClick={() => {
            setStep("phone");
            verifyOtpMutation.reset();
          }}
          className="inline-flex items-center gap-2 text-sm font-bold text-slate-500 transition hover:text-slate-900"
        >
          <ArrowLeft className="size-4" /> Đổi số điện thoại
        </button>
        <div>
          <h1 className="text-3xl font-black tracking-[-0.045em] text-slate-950">
            Kiểm tra điện thoại
          </h1>
          <p className="mt-2 text-sm leading-6 text-slate-500">
            Nhập mã gồm 6 chữ số đã gửi đến{" "}
            <strong className="text-slate-700">{phoneNumber}</strong>.
          </p>
        </div>
        <div>
          <label
            htmlFor="code"
            className="mb-2 block text-sm font-bold text-slate-700"
          >
            Mã xác minh
          </label>
          <input
            id="code"
            inputMode="numeric"
            autoComplete="one-time-code"
            maxLength={6}
            placeholder="000000"
            autoFocus
            {...otpForm.register("code")}
            className="w-full rounded-2xl border border-slate-200 bg-slate-50 px-4 py-4 text-center text-2xl font-bold tracking-wide text-slate-950 outline-none transition focus:border-sky-500 focus:bg-white focus:ring-4 focus:ring-sky-500/10"
          />
          {otpForm.formState.errors.code && (
            <p className="mt-2 text-sm font-semibold text-rose-600">
              {otpForm.formState.errors.code.message}
            </p>
          )}
        </div>
        <FormError
          message={mutationError(
            verifyOtpMutation.error,
            "Không thể xác minh mã OTP",
          )}
        />
        <SubmitButton
          pending={verifyOtpMutation.isPending}
          label="Xác minh và tiếp tục"
        />
      </form>
    );
  }

  return (
    <form
      onSubmit={phoneForm.handleSubmit(sendOtp)}
      className="space-y-5"
      noValidate
    >
      <div>
        <h1 className="text-4xl font-black tracking-[-0.045em] text-slate-950">
          Đăng Nhập
        </h1>
        <p className="mt-2 text-base leading-6 text-slate-500">
          Đăng nhập bằng số điện thoại. Chúng tôi sẽ gửi cho bạn mã xác minh
          dùng một lần.
        </p>
      </div>
      <div>
        <label
          htmlFor="phoneNumber"
          className="mb-2 block text-base font-bold text-slate-700"
        >
          Số điện thoại
        </label>
        <input
          id="phoneNumber"
          type="tel"
          autoComplete="tel"
          placeholder="0862470050"
          autoFocus
          {...phoneForm.register("phoneNumber")}
          className="w-full rounded-2xl border border-slate-200 bg-slate-50 px-4 py-4 text-lg tracking-wide font-semibold text-slate-950 outline-none transition placeholder:text-slate-400 focus:border-sky-500 focus:bg-white focus:ring-4 focus:ring-sky-500/10"
        />
        {phoneForm.formState.errors.phoneNumber && (
          <p className="mt-2 text-base font-semibold text-rose-600">
            {phoneForm.formState.errors.phoneNumber.message}
          </p>
        )}
      </div>
      <FormError
        message={mutationError(sendOtpMutation.error, "Không thể gửi mã OTP")}
      />
      <SubmitButton
        pending={sendOtpMutation.isPending}
        label="Gửi mã xác minh"
      />
      <p className="text-center text-xs leading-5 text-slate-400">
        Khi tiếp tục, bạn đồng ý với điều khoản và chính sách quyền riêng tư của
        PitchUp.
      </p>
    </form>
  );
}

function FormError({ message }: { message: string | null }) {
  return message ? (
    <p
      role="alert"
      className="rounded-xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm font-semibold text-rose-700"
    >
      {message}
    </p>
  ) : null;
}

function mutationError(error: unknown, fallback: string) {
  if (!error) return null;
  return error instanceof Error ? error.message : fallback;
}

function SubmitButton({ pending, label }: { pending: boolean; label: string }) {
  return (
    <button
      type="submit"
      disabled={pending}
      className="flex w-full items-center justify-center gap-2 rounded-2xl bg-slate-950 px-5 py-4 text-sm font-black text-white transition hover:bg-sky-500 hover:text-white disabled:cursor-wait disabled:opacity-60"
    >
      {pending ? (
        <LoaderCircle className="size-5 animate-spin" aria-hidden="true" />
      ) : (
        <ArrowRight className="size-5" aria-hidden="true" />
      )}
      {pending ? "Vui lòng chờ…" : label}
    </button>
  );
}
