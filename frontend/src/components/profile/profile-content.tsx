"use client";

import { useEffect, useState } from "react";
import Image from "next/image";
import Link from "next/link";
import type { Field, PublicProfile, SkillLevel } from "@/lib/api/types";
import {
  Activity,
  BarChart3,
  Camera,
  CalendarDays,
  ChevronLeft,
  ChevronRight,
  Clock,
  Edit3,
  Handshake,
  Bookmark,
  LoaderCircle,
  MapPin,
  Medal,
  Save,
  ShieldCheck,
  Shirt,
  Trophy,
  X,
} from "lucide-react";
import { FavoriteButton } from "@/components/fields/favorite-button";
import {
  useProfile,
  useUpdateProfile,
  useUploadAvatar,
  useUploadTeamPhoto,
} from "@/lib/hooks/use-profile";
import { useFavoriteFields } from "@/lib/hooks/use-fields";
import { DataError, ProfileSkeleton } from "@/components/ui/data-state";
import { formatFieldAddress } from "@/lib/field-format";

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
  AVERAGE: "bg-green-50 text-green-700 ring-green-200",
  ABOVE_AVERAGE: "bg-green-50 text-green-700 ring-green-200",
  GOOD: "bg-green-50 text-green-700 ring-green-200",
  VERY_GOOD: "bg-green-50 text-green-700 ring-green-200",
  SEMI_PRO: "bg-slate-100 text-slate-700 ring-slate-200",
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
  const [bio, setBio] = useState(profile.personal.bio ?? "");
  const [skillLevel, setSkillLevel] = useState<SkillLevel>(
    profile.personal.skillLevel ?? "AVERAGE",
  );
  const update = useUpdateProfile();
  const avatarUpload = useUploadAvatar();
  const teamPhotoUpload = useUploadTeamPhoto();
  const [favoritePage, setFavoritePage] = useState(0);
  const favoriteFields = useFavoriteFields(favoritePage, 4);

  useEffect(() => {
    if (!isOwnProfile || window.location.hash !== "#favorite-fields" || favoriteFields.isPending) return;
    document.getElementById("favorite-fields")?.scrollIntoView({ behavior: "smooth", block: "start" });
  }, [favoriteFields.isPending, isOwnProfile]);

  const displayName = profile.personal.fullName || "Người chơi chưa đặt tên";
  const initial = displayName.slice(0, 1).toUpperCase();

  async function submit(event: React.FormEvent) {
    event.preventDefault();
    await update.mutateAsync({
      fullName: fullName.trim(),
      bio: bio.trim() || null,
      skillLevel,
    });
    setEditing(false);
  }

  return (
    <div className="space-y-7">
      <section className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
        <div className="relative h-56 bg-green-700 sm:h-72 lg:h-80">
          {profile.personal.teamPhotoUrl ? (
            <Image
              src={profile.personal.teamPhotoUrl}
              alt={`Ảnh đội của ${displayName}`}
              fill
              priority
              sizes="(min-width: 1280px) 72rem, 100vw"
              className="object-cover"
            />
          ) : (
            <div className="field-pattern h-full w-full bg-green-700" />
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

        <div className="px-5 pb-6 sm:px-8 sm:pb-8">
          <div className="-mt-16 flex flex-col gap-4 sm:-mt-20 sm:flex-row sm:items-end sm:justify-between">
            <div className="flex flex-col gap-4 sm:flex-row sm:items-end">
              <div className="relative grid size-32 place-items-center overflow-hidden rounded-[1.5rem] bg-slate-100 text-4xl font-black text-slate-700 ring-4 ring-white sm:size-40">
                {profile.personal.avatarUrl ? (
                  <Image
                    src={profile.personal.avatarUrl}
                    alt={`Ảnh đại diện của ${displayName}`}
                    fill
                    sizes="10rem"
                    className="object-cover"
                  />
                ) : (
                  initial
                )}
              </div>
              <div className="pb-1">
                <h1 className="text-3xl font-black text-slate-950">{displayName}</h1>
                <div className="mt-2 flex flex-wrap items-center gap-2">
                  <SkillBadge skillLevel={profile.personal.skillLevel ?? "AVERAGE"} />
                  <span className="inline-flex items-center gap-1.5 rounded-full bg-slate-100 px-3 py-1 text-xs font-black text-slate-600">
                    <ShieldCheck className="size-3.5" />
                    Hồ sơ cầu thủ
                  </span>
                </div>
              </div>
            </div>
            <div className="flex flex-wrap gap-3">
              {isOwnProfile && editing ? (
                <ImageUploadButton
                  id="avatar"
                  label="Ảnh đại diện"
                  pending={avatarUpload.isPending}
                  onFile={(file) => avatarUpload.mutate(file)}
                />
              ) : null}
              {isOwnProfile ? (
                <button
                  type="button"
                  onClick={() => setEditing((value) => !value)}
                  className="action-button border border-slate-200 bg-white text-slate-800"
                >
                  {editing ? <X className="size-4" /> : <Edit3 className="size-4" />}
                  {editing ? "Đóng" : "Chỉnh sửa hồ sơ"}
                </button>
              ) : null}
            </div>
          </div>

          {isOwnProfile && editing ? (
            <form onSubmit={submit} className="mt-7 grid max-w-3xl gap-5">
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
              <Field label="Giới thiệu" htmlFor="bio">
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
            <div className="mt-7 max-w-3xl rounded-2xl border border-slate-100 bg-slate-50 p-5">
              <p className="text-sm font-bold text-slate-500">Giới thiệu</p>
              <p className="mt-2 whitespace-pre-line text-slate-800">
                {profile.personal.bio || "Chưa có giới thiệu."}
              </p>
            </div>
          )}
        </div>
      </section>

      <section className="grid gap-5 lg:grid-cols-[minmax(0,1.35fr)_minmax(22rem,0.65fr)]">
        <StatsGraphPanel profile={profile} />
        <ReputationGraphPanel profile={profile} />
      </section>

      {isOwnProfile ? (
        <FavoriteFieldsSection
          fields={favoriteFields.data?.content ?? []}
          isLoading={favoriteFields.isPending}
          error={favoriteFields.error?.message}
          page={favoriteFields.data?.page ?? favoritePage}
          totalPages={favoriteFields.data?.totalPages ?? 0}
          onPageChange={setFavoritePage}
        />
      ) : null}
    </div>
  );
}

function StatsGraphPanel({ profile }: { profile: PublicProfile }) {
  const matchRows = [
    { label: "Thắng", value: profile.statistics.wins, icon: <Trophy className="size-4" />, className: "bg-green-600" },
    { label: "Hòa", value: profile.statistics.draws, icon: <Handshake className="size-4" />, className: "bg-amber-500" },
    { label: "Thua", value: profile.statistics.losses, icon: <X className="size-4" />, className: "bg-rose-500" },
  ];
  const maxMatches = Math.max(...matchRows.map((row) => row.value), 1);

  return (
    <article className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm sm:p-6">
      <div className="flex items-center justify-between gap-3">
        <div>
          <p className="text-sm font-bold text-slate-500">Thống kê</p>
          <h2 className="mt-1 text-2xl font-black text-slate-950">Hiệu suất thi đấu</h2>
        </div>
        <span className="grid size-11 place-items-center rounded-xl bg-green-50 text-green-700">
          <BarChart3 className="size-5" />
        </span>
      </div>

      <div className="mt-6 grid gap-4 sm:grid-cols-3">
        <MetricCard label="Số trận" value={profile.statistics.totalMatches} icon={<Medal className="size-5" />} />
        <MetricCard label="Tỷ lệ thắng" value={formatPercent(profile.statistics.winRate)} icon={<ShieldCheck className="size-5" />} />
        <MetricCard label="Lịch hoàn thành" value={profile.statistics.completedBookingCount} icon={<CalendarDays className="size-5" />} />
      </div>

      <div className="mt-6 space-y-4">
        {matchRows.map((row) => (
          <GraphBar
            key={row.label}
            label={row.label}
            value={row.value}
            max={maxMatches}
            icon={row.icon}
            barClassName={row.className}
          />
        ))}
      </div>
    </article>
  );
}

function ReputationGraphPanel({ profile }: { profile: PublicProfile }) {
  const rows = [
    { label: "Không hủy lịch", value: profile.reputation.noCancelRate, icon: <ShieldCheck className="size-4" />, className: "bg-green-600" },
    { label: "Đúng giờ", value: profile.reputation.onTimeRate, icon: <Clock className="size-4" />, className: "bg-slate-600" },
    { label: "Fair play", value: profile.reputation.fairPlayRate, icon: <Shirt className="size-4" />, className: "bg-amber-500" },
  ];

  return (
    <article className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm sm:p-6">
      <div className="flex items-center justify-between gap-3">
        <div>
          <p className="text-sm font-bold text-slate-500">Thống kê</p>
          <h2 className="mt-1 text-2xl font-black text-slate-950">Uy tín</h2>
        </div>
        <span className="grid size-11 place-items-center rounded-xl bg-green-50 text-green-700">
          <Activity className="size-5" />
        </span>
      </div>
      <div className="mt-6 space-y-5">
        {rows.map((row) => (
          <GraphBar
            key={row.label}
            label={row.label}
            value={formatPercent(row.value)}
            max={100}
            numericValue={row.value}
            icon={row.icon}
            barClassName={row.className}
          />
        ))}
      </div>
    </article>
  );
}

function FavoriteFieldsSection({
  fields,
  isLoading,
  error,
  page,
  totalPages,
  onPageChange,
}: {
  fields: Field[];
  isLoading: boolean;
  error?: string;
  page: number;
  totalPages: number;
  onPageChange: (page: number) => void;
}) {
  return (
    <section id="favorite-fields" className="scroll-mt-24 rounded-2xl border border-slate-200 bg-white p-5 shadow-sm sm:p-6">
      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div className="flex items-center gap-3">
          <span className="grid size-10 place-items-center rounded-xl bg-green-50 text-green-700">
            <Bookmark className="size-5 fill-current" />
          </span>
          <div>
            <h2 className="text-xl font-black text-slate-950">Sân đã lưu</h2>
            <p className="text-sm font-medium text-slate-500">Các sân bạn đã lưu trong hồ sơ.</p>
          </div>
        </div>
        <Link href="/fields" className="text-sm font-black text-green-700">
          Xem danh sách sân
        </Link>
      </div>

      {isLoading ? <p className="mt-5 text-sm font-semibold text-slate-500">Đang tải sân đã lưu...</p> : null}
      {error ? <p className="mt-5 rounded-xl bg-rose-50 p-3 text-sm font-semibold text-rose-700">{error}</p> : null}
      {!isLoading && !error && fields.length === 0 ? (
        <div className="mt-5 rounded-xl border border-dashed border-slate-200 bg-slate-50 p-5 text-sm text-slate-600">
          Chưa có sân đã lưu.
        </div>
      ) : null}

      <div className="mt-5 grid gap-4 md:grid-cols-2">
        {fields.map((field) => (
          <FavoriteFieldCard key={field.id} field={field} />
        ))}
      </div>
      {totalPages > 1 ? (
        <div className="mt-5 flex items-center justify-center gap-3">
          <button
            type="button"
            disabled={page === 0 || isLoading}
            onClick={() => onPageChange(Math.max(0, page - 1))}
            className="inline-flex items-center gap-2 rounded-full border border-slate-200 px-4 py-2 text-sm font-bold text-slate-700 disabled:opacity-50"
          >
            <ChevronLeft className="size-4" /> Trước
          </button>
          <span className="text-sm font-semibold text-slate-500">
            {page + 1}/{totalPages}
          </span>
          <button
            type="button"
            disabled={page + 1 >= totalPages || isLoading}
            onClick={() => onPageChange(page + 1)}
            className="inline-flex items-center gap-2 rounded-full bg-slate-950 px-4 py-2 text-sm font-bold text-white disabled:opacity-50"
          >
            Sau <ChevronRight className="size-4" />
          </button>
        </div>
      ) : null}
    </section>
  );
}

function FavoriteFieldCard({ field }: { field: Field }) {
  const primaryImage =
    field.images?.find((image) => image.isPrimary)?.imageUrl ?? field.images?.[0]?.imageUrl;

  return (
    <article className="group overflow-hidden rounded-xl border border-slate-200 bg-slate-50 transition hover:border-green-300 hover:bg-white hover:shadow-md">
      <div className="grid gap-0 sm:grid-cols-[9rem_minmax(0,1fr)]">
        <Link href={`/fields/${field.id}`} className="relative block aspect-[16/10] bg-slate-200 sm:aspect-auto sm:min-h-36">
          {primaryImage ? (
            <Image
              src={primaryImage}
              alt={field.name}
              fill
              sizes="(max-width: 768px) 100vw, 9rem"
              className="object-cover transition duration-300 group-hover:scale-105"
            />
          ) : (
            <div className="field-pattern h-full w-full" />
          )}
        </Link>
        <div className="flex min-w-0 flex-col justify-between gap-4 p-4">
          <div className="flex items-start justify-between gap-3">
            <div className="min-w-0">
              <Link href={`/fields/${field.id}`} className="line-clamp-1 text-base font-black text-slate-950 hover:text-green-700">
                {field.name}
              </Link>
              <p className="mt-2 flex items-start gap-2 text-sm leading-6 text-slate-500">
                <MapPin className="mt-1 size-4 shrink-0 text-green-600" />
                <span className="line-clamp-2">{formatFieldAddress(field)}</span>
              </p>
            </div>
            <FavoriteButton
              fieldId={field.id}
              isSaved={field.isSaved ?? field.isFavorite ?? true}
              className="inline-grid size-10 shrink-0 place-items-center rounded-full border border-green-100 bg-white text-lg text-green-600 shadow-sm transition hover:scale-105 disabled:cursor-wait disabled:opacity-70"
            />
          </div>
          <div className="flex items-center justify-between border-t border-slate-200 pt-3 text-sm">
            <span className="font-bold text-slate-600">{Number(field.ratingAverage ?? 0).toFixed(1)} điểm</span>
            <Link href={`/fields/${field.id}`} className="font-black text-green-700">
              Xem sân
            </Link>
          </div>
        </div>
      </div>
    </article>
  );
}

function SkillBadge({ skillLevel }: { skillLevel: SkillLevel }) {
  return (
    <span className={`inline-flex rounded-full px-3 py-1 text-xs font-black ring-1 ${skillBadgeClasses[skillLevel]}`}>
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
    <div className="rounded-xl border border-slate-100 bg-slate-50 p-4">
      <div className="flex items-center justify-between gap-3">
        <span className="text-xs font-bold uppercase tracking-wide text-slate-500">{label}</span>
        <span className="grid size-8 place-items-center rounded-lg bg-white text-slate-600 shadow-sm">
          {icon}
        </span>
      </div>
      <p className="mt-3 text-2xl font-black text-slate-950">{value}</p>
    </div>
  );
}

function GraphBar({
  label,
  value,
  max,
  numericValue,
  icon,
  barClassName,
}: {
  label: string;
  value: string | number;
  max: number;
  numericValue?: number;
  icon: React.ReactNode;
  barClassName: string;
}) {
  const amount = Number(numericValue ?? value ?? 0);
  const width = max > 0 ? Math.max(4, Math.min(100, (amount / max) * 100)) : 4;

  return (
    <div>
      <div className="mb-2 flex items-center justify-between gap-3 text-sm">
        <span className="flex items-center gap-2 font-bold text-slate-700">
          <span className="grid size-7 place-items-center rounded-lg bg-slate-100 text-slate-600">
            {icon}
          </span>
          {label}
        </span>
        <span className="font-black text-slate-950">{value}</span>
      </div>
      <div className="h-3 overflow-hidden rounded-full bg-slate-100">
        <div className={`h-full rounded-full ${barClassName}`} style={{ width: `${width}%` }} />
      </div>
    </div>
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
        <p className="rounded-xl bg-green-50 p-3 text-sm font-semibold text-green-700">
          Hồ sơ đã được cập nhật.
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
