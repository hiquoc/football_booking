"use client";

import { useRouter } from "next/navigation";
import { useMemo, useState, type FormEvent } from "react";
import { LoaderCircle } from "lucide-react";
import type { PublicProfile } from "@/lib/api/types";
import { useMyBookings } from "@/lib/hooks/use-bookings";
import { useCreateCommunityPost } from "@/lib/hooks/use-community";

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
    <div className="mx-auto w-full max-w-3xl px-5 py-10 sm:px-8">
      <h1 className="text-3xl font-black text-slate-950">Đăng bài cộng đồng</h1>
      <p className="mt-2 text-slate-600">Chọn một lịch đặt sân đã xác nhận. Thông tin sân và khung giờ sẽ được khóa theo lịch đặt.</p>
      <form onSubmit={submit} className="mt-6 space-y-4 rounded-lg border border-slate-200 bg-white p-5 shadow-sm">
        <label className="block text-sm font-bold text-slate-700">
          Lịch đặt sân
          <select name="bookingId" required className="mt-1.5 w-full rounded-lg border border-slate-200 px-3 py-2">
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
          <select value={postType} onChange={(event) => setPostType(event.target.value as typeof postType)} className="mt-1.5 w-full rounded-lg border border-slate-200 px-3 py-2">
            <option value="LOOKING_OPPONENT">Tìm đối thủ</option>
            <option value="LOOKING_PLAYER">Tìm thêm cầu thủ</option>
          </select>
        </label>
        <Input name="title" label="Tiêu đề" required />
        <label className="block text-sm font-bold text-slate-700">
          Mô tả
          <textarea name="description" rows={4} className="mt-1.5 w-full rounded-lg border border-slate-200 px-3 py-2" />
        </label>
        <div className="grid gap-4 sm:grid-cols-2">
          <Input name="skillLevel" label="Trình độ" defaultValue={profile?.personal.skillLevel ?? "AVERAGE"} required />
          <Input name="contactPhone" label="Zalo" defaultValue={profile?.personal.phoneNumber ?? ""} required />
        </div>
        {postType === "LOOKING_PLAYER" ? <Input name="playersNeeded" label="Số cầu thủ cần thêm" type="number" min={1} required /> : null}
        <button disabled={create.isPending || bookings.isPending} className="inline-flex items-center gap-2 rounded-lg bg-emerald-600 px-5 py-3 text-sm font-black text-white disabled:opacity-60">
          {create.isPending ? <LoaderCircle className="size-4 animate-spin" /> : null} Đăng bài
        </button>
        {create.error ? <p className="text-sm font-semibold text-rose-600">{create.error.message}</p> : null}
      </form>
    </div>
  );
}

function Input(props: React.InputHTMLAttributes<HTMLInputElement> & { label: string }) {
  const { label, ...inputProps } = props;
  return (
    <label className="block text-sm font-bold text-slate-700">
      {label}
      <input {...inputProps} className="mt-1.5 w-full rounded-lg border border-slate-200 px-3 py-2" />
    </label>
  );
}
