"use client";

import { useRouter } from "next/navigation";
import { useMemo, useState, type FormEvent } from "react";
import { LoaderCircle } from "lucide-react";
import { BackLink } from "@/components/ui/back-link";
import type { PublicProfile } from "@/lib/api/types";
import { useMyBookings } from "@/lib/hooks/use-bookings";
import { useCreateCommunityPost } from "@/lib/hooks/use-community";
import { skillLevelOptions } from "./community-labels";

export function CommunityCreateContent({ profile }: { profile: PublicProfile | null }) {
  const router = useRouter();
  const bookings = useMyBookings(0, 30);
  const create = useCreateCommunityPost();
  const [postType, setPostType] = useState<"LOOKING_OPPONENT" | "LOOKING_PLAYER">("LOOKING_OPPONENT");
  const confirmedBookings = useMemo(
    () => bookings.data?.content.filter((booking) => booking.status === "CONFIRMED") ?? [],
    [bookings.data],
  );

  const submit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    const playersNeeded = Number(form.get("playersNeeded"));
    create.mutate(
      {
        bookingId: String(form.get("bookingId")),
        postType,
        title: String(form.get("title")).trim(),
        description: String(form.get("description")).trim(),
        skillLevel: String(form.get("skillLevel")),
        contactPhone: String(form.get("contactPhone")).trim(),
        playersNeeded: postType === "LOOKING_PLAYER" ? playersNeeded : undefined,
        ownerDisplayName: profile?.personal.fullName,
        ownerAvatarUrl: profile?.personal.avatarUrl,
        ownerTeamPhotoUrl: profile?.personal.teamPhotoUrl,
      },
      { onSuccess: (post) => router.push(`/community/${post.id}`) },
    );
  };

  return (
    <div className="mx-auto w-full max-w-4xl px-5 py-10 sm:px-8">
      <BackLink href="/community" className="mb-5">Quay lại cộng đồng</BackLink>
      <header className="mb-6">
        <p className="text-sm font-bold uppercase tracking-wider text-green-600">Cộng đồng</p>
        <h1 className="mt-2 text-3xl font-black text-slate-950">Đăng bài cộng đồng</h1>
        <p className="mt-2 max-w-2xl text-slate-600">
          Chọn một lịch đặt sân đã xác nhận. Thông tin sân và khung giờ sẽ được khóa theo lịch đặt.
        </p>
      </header>

      <form onSubmit={submit} className="space-y-4 rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
        <label className="block text-sm font-bold text-slate-700">
          Lịch đặt sân
          <select name="bookingId" required className={inputClassName}>
            <option value="">Chọn lịch đặt</option>
            {confirmedBookings.map((booking) => (
              <option key={booking.id} value={booking.id}>
                {booking.fieldName} · {booking.subFieldName} · {booking.bookingDate} {booking.startTime.slice(0, 5)}
              </option>
            ))}
          </select>
        </label>

        <label className="block text-sm font-bold text-slate-700">
          Mục đích
          <select value={postType} onChange={(event) => setPostType(event.target.value as typeof postType)} className={inputClassName}>
            <option value="LOOKING_OPPONENT">Tìm đối thủ</option>
            <option value="LOOKING_PLAYER">Tìm thêm cầu thủ</option>
          </select>
        </label>

        <Input name="title" label="Tiêu đề" required />
        <label className="block text-sm font-bold text-slate-700">
          Mô tả
          <textarea name="description" rows={4} className={inputClassName} />
        </label>

        <div className="grid gap-4 sm:grid-cols-2">
          <label className="block text-sm font-bold text-slate-700">
            Trình độ
            <select name="skillLevel" defaultValue={profile?.personal.skillLevel ?? "AVERAGE"} required className={inputClassName}>
              {skillLevelOptions.map(([value, label]) => (
                <option key={value} value={value}>{label}</option>
              ))}
            </select>
          </label>
          <Input name="contactPhone" label="Zalo" defaultValue={profile?.personal.phoneNumber ?? ""} required />
        </div>

        {postType === "LOOKING_PLAYER" ? <Input name="playersNeeded" label="Số cầu thủ cần thêm" type="number" min={1} required /> : null}

        <button disabled={create.isPending || bookings.isPending} className="action-button bg-green-600 px-5 text-white hover:bg-green-700 disabled:opacity-60">
          {create.isPending ? <LoaderCircle className="size-4 animate-spin" /> : null} Đăng bài
        </button>
        {create.error ? <p className="text-sm font-semibold text-rose-600">{create.error.message}</p> : null}
      </form>
    </div>
  );
}

const inputClassName = "mt-1.5 w-full rounded-xl border border-slate-200 px-3 py-2.5 text-sm outline-none focus:border-green-500 focus:ring-4 focus:ring-green-100";

function Input(props: React.InputHTMLAttributes<HTMLInputElement> & { label: string }) {
  const { label, ...inputProps } = props;
  return (
    <label className="block text-sm font-bold text-slate-700">
      {label}
      <input {...inputProps} className={inputClassName} />
    </label>
  );
}
