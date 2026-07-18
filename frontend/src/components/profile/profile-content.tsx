"use client";

import { useState } from "react";
import Image from "next/image";
import type { PublicProfile, SkillLevel } from "@/lib/api/types";
import {
  Camera,
  CalendarDays,
  Clock,
  Edit3,
  Handshake,
  LoaderCircle,
  Medal,
  Phone,
  Save,
  ShieldCheck,
  Shirt,
  Trophy,
  UserRound,
  X,
} from "lucide-react";
import {
  useProfile,
  useUpdateProfile,
  useUploadAvatar,
  useUploadTeamPhoto,
} from "@/lib/hooks/use-profile";
import { DataError, ProfileSkeleton } from "@/components/ui/data-state";

const skillLabels: Record<SkillLevel, string> = {
  VERY_WEAK: "Rất yếu",
  WEAK: "Yếu",
  AVERAGE: "Trung bình",
  ABOVE_AVERAGE: "Khá",
  GOOD: "Tốt",
  VERY_GOOD: "Rất tốt",
  SEMI_PRO: "Bán chuyên",
  PRO: "Chuyên nghiệp",
};

const skillBadgeClasses: Record<SkillLevel, string> = {
  VERY_WEAK: "bg-slate-100 text-slate-700 ring-slate-200",
  WEAK: "bg-amber-50 text-amber-700 ring-amber-200",
  AVERAGE: "bg-sky-50 text-sky-700 ring-sky-200",
  ABOVE_AVERAGE: "bg-cyan-50 text-cyan-700 ring-cyan-200",
  GOOD: "bg-emerald-50 text-emerald-700 ring-emerald-200",
  VERY_GOOD: "bg-teal-50 text-teal-700 ring-teal-200",
  SEMI_PRO: "bg-violet-50 text-violet-700 ring-violet-200",
  PRO: "bg-rose-50 text-rose-700 ring-rose-200",
};

const skillOptions = Object.keys(skillLabels) as SkillLevel[];

export function ProfileContent({
  userId,
  isOwnProfile = true,
}: {
  userId?: string;
  isOwnProfile?: boolean;
}) {
  const profile = useProfile(isOwnProfile ? undefined : userId);

  if (profile.isPending) return <ProfileSkeleton />;
  if (profile.isError) return <DataError title="Không thể tải hồ sơ" />;

  return (
    <ProfileView
      key={`${profile.data.personal.id}:${profile.data.updatedAt ?? ""}`}
      profile={profile.data}
      isOwnProfile={isOwnProfile}
    />
  );
}

function ProfileView({
  profile,
  isOwnProfile,
}: {
  profile: PublicProfile;
  isOwnProfile: boolean;
}) {
  const [editing, setEditing] = useState(false);
  const [fullName, setFullName] = useState(profile.personal.fullName ?? "");
  const [phoneNumber, setPhoneNumber] = useState(profile.personal.phoneNumber ?? "");
  const [bio, setBio] = useState(profile.personal.bio ?? "");
  const [skillLevel, setSkillLevel] = useState<SkillLevel>(
    profile.personal.skillLevel ?? "AVERAGE",
  );
  const update = useUpdateProfile();
  const avatarUpload = useUploadAvatar();
  const teamPhotoUpload = useUploadTeamPhoto();

  const displayName = profile.personal.fullName || "Chưa cập nhật tên";
  const initial = (displayName || profile.personal.phoneNumber || "U").slice(0, 1).toUpperCase();

  async function submit(event: React.FormEvent) {
    event.preventDefault();
    await update.mutateAsync({
      fullName: fullName.trim(),
      phoneNumber: phoneNumber.trim(),
      bio: bio.trim() || null,
      skillLevel,
    });
    setEditing(false);
  }

  return (
    <div className="space-y-7">
      <section className="grid gap-7 lg:grid-cols-[20rem_minmax(0,1fr)]">
        <aside className="h-fit overflow-hidden rounded-[2rem] border border-slate-200 bg-white shadow-sm">
          <div className="relative h-36 bg-emerald-700">
            {profile.personal.teamPhotoUrl ? (
              <Image
                src={profile.personal.teamPhotoUrl}
                alt={`Ảnh đội của ${displayName}`}
                fill
                sizes="(min-width: 1024px) 20rem, 100vw"
                className="object-cover"
              />
            ) : (
              <div className="field-pattern h-full w-full bg-emerald-700" />
            )}
            {isOwnProfile && editing ? (
              <ImageUploadButton
                id="teamPhoto"
                label="Ảnh đội"
                pending={teamPhotoUpload.isPending}
                onFile={(file) => teamPhotoUpload.mutate(file)}
                className="absolute right-4 top-4"
              />
            ) : null}
          </div>
          <div className="px-6 pb-6">
            <div className="-mt-12 flex items-end justify-between gap-4">
              <div className="relative grid size-28 place-items-center overflow-hidden rounded-[1.5rem] bg-slate-100 text-3xl font-black text-slate-700 ring-4 ring-white">
                {profile.personal.avatarUrl ? (
                  <Image
                    src={profile.personal.avatarUrl}
                    alt={`Ảnh đại diện của ${displayName}`}
                    fill
                    sizes="7rem"
                    className="object-cover"
                  />
                ) : (
                  initial
                )}
              </div>
              {isOwnProfile && editing ? (
                <ImageUploadButton
                  id="avatar"
                  label="Ảnh hồ sơ"
                  pending={avatarUpload.isPending}
                  onFile={(file) => avatarUpload.mutate(file)}
                />
              ) : null}
            </div>

            <div className="mt-5">
              <h2 className="text-2xl font-black text-slate-950">{displayName}</h2>
              <SkillBadge skillLevel={profile.personal.skillLevel ?? "AVERAGE"} />
            </div>

            <div className="mt-6 space-y-3 border-t border-slate-100 pt-5 text-sm text-slate-600">
              <p className="flex items-center gap-2">
                <Phone className="size-4 text-slate-400" />
                {profile.personal.phoneNumber || "Chưa cập nhật"}
              </p>
              <p className="flex items-center gap-2">
                <ShieldCheck className="size-4 text-slate-400" />
                Hồ sơ cầu thủ
              </p>
            </div>
          </div>
        </aside>

        <section className="rounded-[2rem] border border-slate-200 bg-white p-6 shadow-sm sm:p-8">
          <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
            <div className="flex items-center gap-3">
              <span className="grid size-10 place-items-center rounded-xl bg-emerald-50 text-emerald-700">
                <UserRound className="size-5" />
              </span>
              <h1 className="text-2xl font-black text-slate-950">Hồ sơ cầu thủ</h1>
            </div>
            {isOwnProfile ? (
              <button
                type="button"
                onClick={() => setEditing((value) => !value)}
                className="action-button border border-slate-200 bg-white text-slate-800"
              >
                {editing ? <X className="size-4" /> : <Edit3 className="size-4" />}
                {editing ? "Đóng" : "Edit Profile"}
              </button>
            ) : null}
          </div>

          {isOwnProfile && editing ? (
            <form onSubmit={submit} className="mt-7 grid gap-5">
              <Field label="Họ và tên" htmlFor="fullName">
                <input
                  id="fullName"
                  className="input-field"
                  value={fullName}
                  onChange={(event) => setFullName(event.target.value)}
                  minLength={2}
                  maxLength={100}
                />
              </Field>
              <Field label="Số điện thoại" htmlFor="phoneNumber">
                <input
                  id="phoneNumber"
                  className="input-field"
                  value={phoneNumber}
                  onChange={(event) => setPhoneNumber(event.target.value)}
                  maxLength={20}
                />
              </Field>
              <Field label="Trình độ" htmlFor="skillLevel">
                <select
                  id="skillLevel"
                  className="input-field"
                  value={skillLevel}
                  onChange={(event) => setSkillLevel(event.target.value as SkillLevel)}
                >
                  {skillOptions.map((option) => (
                    <option key={option} value={option}>
                      {skillLabels[option]}
                    </option>
                  ))}
                </select>
              </Field>
              <Field label="Bio" htmlFor="bio">
                <textarea
                  id="bio"
                  className="input-field min-h-32 resize-y"
                  value={bio}
                  onChange={(event) => setBio(event.target.value)}
                  maxLength={500}
                />
              </Field>
              <StatusMessages
                updateError={update.error?.message}
                avatarError={avatarUpload.error?.message}
                teamPhotoError={teamPhotoUpload.error?.message}
                success={update.isSuccess}
              />
              <button
                disabled={update.isPending}
                className="action-button w-fit bg-slate-950 px-6 text-white disabled:opacity-60"
              >
                {update.isPending ? (
                  <LoaderCircle className="size-4 animate-spin" />
                ) : (
                  <Save className="size-4" />
                )}
                Lưu thay đổi
              </button>
            </form>
          ) : (
            <div className="mt-7 rounded-2xl border border-slate-100 bg-slate-50 p-5">
              <p className="text-sm font-bold text-slate-500">Bio</p>
              <p className="mt-2 whitespace-pre-line text-slate-800">
                {profile.personal.bio || "Chưa cập nhật"}
              </p>
            </div>
          )}
        </section>
      </section>

      <section className="grid gap-4 sm:grid-cols-2 lg:grid-cols-5">
        <MetricCard label="Matches" value={profile.statistics.totalMatches} icon={<Medal className="size-5" />} />
        <MetricCard label="Wins" value={profile.statistics.wins} icon={<Trophy className="size-5" />} />
        <MetricCard label="Draws" value={profile.statistics.draws} icon={<Handshake className="size-5" />} />
        <MetricCard label="Losses" value={profile.statistics.losses} icon={<X className="size-5" />} />
        <MetricCard label="Win Rate" value={formatPercent(profile.statistics.winRate)} icon={<ShieldCheck className="size-5" />} />
      </section>

      <section className="grid gap-4 md:grid-cols-1">
        <MetricCard label="Completed Bookings" value={profile.statistics.completedBookingCount} icon={<CalendarDays className="size-5" />} />
      </section>

      <section className="grid gap-4 md:grid-cols-3">
        <MetricCard label="No Cancel" value={formatPercent(profile.reputation.noCancelRate)} icon={<ShieldCheck className="size-5" />} />
        <MetricCard label="On Time" value={formatPercent(profile.reputation.onTimeRate)} icon={<Clock className="size-5" />} />
        <MetricCard label="Fair Play" value={formatPercent(profile.reputation.fairPlayRate)} icon={<Shirt className="size-5" />} />
      </section>
    </div>
  );
}

function SkillBadge({ skillLevel }: { skillLevel: SkillLevel }) {
  return (
    <span className={`mt-3 inline-flex rounded-full px-3 py-1 text-xs font-black ring-1 ${skillBadgeClasses[skillLevel]}`}>
      {skillLabels[skillLevel]}
    </span>
  );
}

function MetricCard({
  label,
  value,
  icon,
}: {
  label: string;
  value: string | number;
  icon: React.ReactNode;
}) {
  return (
    <article className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
      <div className="flex items-center justify-between gap-3">
        <span className="text-sm font-bold text-slate-500">{label}</span>
        <span className="grid size-9 place-items-center rounded-xl bg-slate-100 text-slate-600">
          {icon}
        </span>
      </div>
      <p className="mt-4 text-3xl font-black text-slate-950">{value}</p>
    </article>
  );
}

function ImageUploadButton({
  id,
  label,
  pending,
  onFile,
  className = "",
}: {
  id: string;
  label: string;
  pending: boolean;
  onFile: (file: File) => void;
  className?: string;
}) {
  return (
    <label
      htmlFor={id}
      className={`action-button cursor-pointer bg-slate-950 px-4 text-white ${className}`}
    >
      {pending ? <LoaderCircle className="size-4 animate-spin" /> : <Camera className="size-4" />}
      {label}
      <input
        id={id}
        className="sr-only"
        type="file"
        accept="image/jpeg,image/png,image/webp"
        disabled={pending}
        onChange={(event) => {
          const file = event.target.files?.[0];
          if (file) onFile(file);
        }}
      />
    </label>
  );
}

function StatusMessages({
  updateError,
  avatarError,
  teamPhotoError,
  success,
}: {
  updateError?: string;
  avatarError?: string;
  teamPhotoError?: string;
  success: boolean;
}) {
  const error = updateError ?? avatarError ?? teamPhotoError;
  return (
    <>
      {success ? (
        <p className="rounded-xl bg-emerald-50 p-3 text-sm font-semibold text-emerald-700">
          Đã cập nhật hồ sơ.
        </p>
      ) : null}
      {error ? (
        <p className="rounded-xl bg-rose-50 p-3 text-sm text-rose-700">{error}</p>
      ) : null}
    </>
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

function formatPercent(value: number) {
  return `${Number(value ?? 0).toFixed(1).replace(/\.0$/, "")}%`;
}
