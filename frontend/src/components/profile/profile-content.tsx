"use client";

import { useState } from "react";
import type { User } from "@/lib/api/types";
import {
  Camera,
  LoaderCircle,
  Phone,
  Save,
  ShieldCheck,
  UserRound,
} from "lucide-react";
import { useProfile, useUpdateProfile, useUploadAvatar } from "@/lib/hooks/use-profile";
import { DataError, ListSkeleton } from "@/components/ui/data-state";

const roleNames = {
  CLIENT: "Người chơi",
  OWNER: "Chủ sân",
  ADMIN: "Quản trị viên",
};

export function ProfileContent() {
  const profile = useProfile();

  if (profile.isPending) return <ListSkeleton count={2} />;
  if (profile.isError) return <DataError title="Không thể tải hồ sơ" />;
  return (
    <ProfileForm
      key={profile.data.updatedAt ?? profile.data.id}
      user={profile.data}
    />
  );
}

function ProfileForm({ user }: { user: User }) {
  const update = useUpdateProfile();
  const avatarUpload = useUploadAvatar();
  const [fullName, setFullName] = useState(user.fullName ?? "");
  const displayName = user.fullName || "Chưa cập nhật tên";
  const initial = (user.fullName || user.phoneNumber).slice(0, 1).toUpperCase();

  async function submit(event: React.FormEvent) {
    event.preventDefault();
    try {
      await update.mutateAsync({
        fullName: fullName.trim(),
      });
    } catch {
      /* Mutation state renders the error. */
    }
  }

  return (
    <div className="grid gap-7 lg:grid-cols-[19rem_minmax(0,1fr)]">
      <aside className="relative h-fit overflow-hidden rounded-[2rem] border border-sky-100 bg-gradient-to-br from-sky-50 via-white to-cyan-50 p-6 text-slate-900 shadow-sm">
        <div className="absolute -right-12 -top-12 size-36 rounded-full bg-sky-100/70 blur-2xl" />
        {user.avatarUrl ? (
          <img
            src={user.avatarUrl}
            alt={`Ảnh đại diện của ${displayName}`}
            className="relative grid size-20 place-items-center rounded-3xl object-cover object-center ring-4 ring-white shadow-md"
            loading="lazy"
          />
        ) : (
          <div
            className="relative grid size-20 place-items-center rounded-3xl bg-sky-100 text-2xl font-black text-sky-700 ring-4 ring-white shadow-md"
            aria-label={`Chữ cái đại diện của ${displayName}`}
            role="img"
          >
            {/* Optional: Add first letter of name here if image fails */}
            {displayName?.charAt(0).toUpperCase()}
          </div>
        )}

        <h2 className="mt-5 text-xl font-black">
          {displayName}
        </h2>
        <p className="mt-1 text-sm font-medium text-slate-500">
          {roleNames[user.userType]}
        </p>
        <div className="mt-6 space-y-3 border-t border-slate-100 pt-5 text-sm text-slate-500">
          <p className="flex items-center gap-2">
            <Phone className="size-4" /> {user.phoneNumber}
          </p>
          <p className="flex items-center gap-2">
            <ShieldCheck className="size-4" />{" "}
            {user.status === "ACTIVE"
              ? "Tài khoản đang hoạt động"
              : user.status}
          </p>
        </div>
      </aside>
      <form
        onSubmit={submit}
        className="rounded-[2rem] border border-slate-200/80 bg-white p-6 shadow-[0_20px_60px_-32px_rgba(15,23,42,0.28)] sm:p-8"
      >
        <div className="flex items-center gap-3">
          <span className="grid size-10 place-items-center rounded-xl bg-sky-100 text-sky-700">
            <UserRound className="size-5" />
          </span>
          <div>
            <h1 className="text-2xl font-black text-slate-950">
              Thông tin cá nhân
            </h1>
            <p className="text-sm text-slate-500">
              Cập nhật tên và ảnh đại diện của bạn.
            </p>
          </div>
        </div>
        <div className="mt-7 space-y-5 rounded-2xl border border-slate-100 bg-slate-50/60 p-4 sm:p-5">
          <Field label="Họ và tên" htmlFor="fullName">
            <input
              id="fullName"
              className="input-field"
              value={fullName}
              onChange={(event) => setFullName(event.target.value)}
              minLength={2}
              maxLength={100}
              placeholder="Nguyễn Văn A"
            />
          </Field>
          <Field label="Số điện thoại" htmlFor="phoneNumber">
            <input
              id="phoneNumber"
              className="input-field cursor-not-allowed text-slate-400"
              value={user.phoneNumber}
              disabled
            />
          </Field>
          <Field label="Ảnh đại diện" htmlFor="avatar">
            <div className="flex flex-col gap-4 rounded-2xl border border-dashed border-sky-200 bg-white p-4 sm:flex-row sm:items-center">
              <span
                className="grid size-16 shrink-0 place-items-center rounded-2xl bg-sky-100 bg-cover bg-center text-xl font-black text-sky-700"
                style={user.avatarUrl ? { backgroundImage: `url(${user.avatarUrl})` } : undefined}
              >
                {user.avatarUrl ? null : initial}
              </span>
              <div className="min-w-0 flex-1">
                <p className="text-sm font-bold text-slate-800">Chọn ảnh hồ sơ mới</p>
                <p className="mt-1 text-xs leading-5 text-slate-500">JPG, PNG hoặc WebP. Ảnh vuông sẽ hiển thị đẹp nhất.</p>
              </div>
              <label
                htmlFor="avatar"
                className="inline-flex min-h-11 cursor-pointer items-center justify-center gap-2 rounded-full bg-sky-600 px-4 text-sm font-black text-white shadow-sm transition hover:bg-sky-700"
              >
                {avatarUpload.isPending ? <LoaderCircle className="size-4 animate-spin" /> : <Camera className="size-4" />}
                {avatarUpload.isPending ? "Đang tải..." : "Chọn ảnh"}
              </label>
              <input
                id="avatar"
                className="sr-only"
                type="file"
                accept="image/jpeg,image/png,image/webp"
                disabled={avatarUpload.isPending}
                onChange={(event) => {
                  const file = event.target.files?.[0];
                  if (file) avatarUpload.mutate(file);
                }}
              />
            </div>
          </Field>
        </div>
        {update.isSuccess ? (
          <p className="mt-5 rounded-xl bg-sky-50 p-3 text-sm font-semibold text-sky-700">
            Đã cập nhật hồ sơ.
          </p>
        ) : null}
        {update.error ? (
          <p className="mt-5 rounded-xl bg-rose-50 p-3 text-sm text-rose-700">
            {update.error.message}
          </p>
        ) : null}
        {avatarUpload.error ? (
          <p className="mt-5 rounded-xl bg-rose-50 p-3 text-sm text-rose-700">
            {avatarUpload.error.message}
          </p>
        ) : null}
        <button
          disabled={update.isPending}
          className="mt-6 inline-flex min-h-12 items-center gap-2 rounded-full bg-slate-950 px-6 py-3 text-sm font-black text-white disabled:opacity-60"
        >
          {update.isPending ? (
            <LoaderCircle className="size-4 animate-spin" />
          ) : (
            <Save className="size-4" />
          )}{" "}
          Lưu thay đổi
        </button>
      </form>
    </div>
  );
}

function Field({
  label,
  htmlFor,
  children,
}: {
  label: string;
  htmlFor: string;
  children: React.ReactNode;
}) {
  return (
    <div>
      <label htmlFor={htmlFor} className="mb-2 block text-sm font-bold text-slate-700">
        {label}
      </label>
      {children}
    </div>
  );
}
