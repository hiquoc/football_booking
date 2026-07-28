"use client";

import Link from "next/link";
import {
  CircleAlert,
  Layers3,
  MapPin,
  Settings2,
  Star,
} from "lucide-react";
import { FieldStatusControl } from "@/components/admin/field-status-control";
import { BackLink } from "@/components/ui/back-link";
import type { Field, SubField, User } from "@/lib/api/types";
import { formatEnum, formatFieldAddress } from "@/lib/field-format";
import { useFieldDetails } from "@/lib/hooks/use-fields";
import { FieldContactCard } from "./field-contact-card";
import { FavoriteButton } from "./favorite-button";
import { FieldGallery } from "./field-gallery";
import { OperatingHours } from "./operating-hours";
import { ReviewForm } from "./review-form";
import { ReviewList } from "./review-list";
import { SubFieldList } from "./sub-field-list";

export function FieldDetailContent({
  fieldId,
  viewerRole,
  viewerUserId,
}: {
  fieldId: string;
  viewerRole: User["userType"] | null;
  viewerUserId: string | null;
}) {
  const details = useFieldDetails(fieldId);

  if (details.isPending) return <FieldDetailSkeleton />;
  if (details.isError) return <FieldDetailError />;
  const { field, operatingHours, subFields, reviews } = details.data;

  return (
    <div className="pb-20">
      <div className="mx-auto w-full max-w-[90rem] px-5 py-8 sm:px-8">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <BackLink href="/fields">Tất cả sân</BackLink>
          <FieldDetailActions
            fieldId={fieldId}
            fieldOwnerId={field.ownerId}
            isSaved={field.isSaved ?? field.isFavorite}
            viewerRole={viewerRole}
            viewerUserId={viewerUserId}
          />
        </div>
        <div className="mt-6">
          <FieldGallery images={field.images} name={field.name} />
        </div>

        <div className="mt-9 grid gap-10 lg:grid-cols-[minmax(0,1fr)_22rem]">
          <div>
            <FieldOverview field={field} subFields={subFields} />
            <div className="mt-10">
              <SubFieldList
                fields={subFields}
                isBookable={field.status === "APPROVED"}
              />
            </div>
            <div className="mt-12">
              <ReviewList reviews={reviews} />
              <ReviewForm fieldId={fieldId} />
            </div>
          </div>
          <aside className="space-y-5 lg:sticky lg:top-24 lg:self-start">
            {viewerRole === "ADMIN" ? (
              <div className="rounded-3xl border border-slate-200 bg-white p-5 shadow-sm">
                <h2 className="text-lg font-black text-slate-950">
                  Đổi trạng thái sân
                </h2>
                <div className="mt-4">
                  <FieldStatusControl fieldId={fieldId} status={field.status} />
                </div>
              </div>
            ) : null}
            <OperatingHours hours={operatingHours} />
            <FieldContactCard field={field} />
          </aside>
        </div>
      </div>
    </div>
  );
}

function FieldDetailActions({
  fieldId,
  fieldOwnerId,
  isSaved,
  viewerRole,
  viewerUserId,
}: {
  fieldId: string;
  fieldOwnerId: string;
  isSaved?: boolean;
  viewerRole: User["userType"] | null;
  viewerUserId: string | null;
}) {
  if (viewerRole === "OWNER" && viewerUserId === fieldOwnerId) {
    return (
      <Link
        href={`/owner/fields/${fieldId}/edit`}
        className="inline-flex items-center gap-2 rounded-full border border-slate-200 bg-white px-4 py-2 text-sm font-black text-slate-700 transition hover:border-sky-400 hover:text-sky-700"
      >
        <Settings2 className="size-4" /> Quản lý sân
      </Link>
    );
  }

  if (viewerRole === "ADMIN") {
    return null;
  }

  if (viewerRole === "CLIENT" || viewerRole === "EMPLOYEE") {
    return (
      <FavoriteButton
        fieldId={fieldId}
        isSaved={isSaved}
        className="inline-grid size-11 place-items-center rounded-full border border-slate-200 bg-white text-xl shadow-sm transition hover:border-rose-300 hover:bg-rose-50 disabled:cursor-wait disabled:opacity-70"
      />
    );
  }

  return null;
}

function FieldOverview({
  field,
  subFields,
}: {
  field: Field;
  subFields?: SubField[];
}) {
  const activeSubFields = subFields?.filter((item) => item.active) ?? [];
  const subtypeCounts = activeSubFields.reduce<Record<string, number>>(
    (counts, item) => ({
      ...counts,
      [item.subFieldType]: (counts[item.subFieldType] ?? 0) + 1,
    }),
    {},
  );

  return (
    <>
      <div className="flex flex-wrap gap-2">
        {field.fieldTypes
          ?.map((type) => (
            <span
              key={type.id}
              className="rounded-full bg-sky-100 px-3 py-1.5 text-xs font-black uppercase tracking-[0.1em] text-sky-700"
            >
              {formatEnum(type.name)}
            </span>
          ))
          .sort((a, b) =>
            a.props.children === "Bóng đá"
              ? -1
              : b.props.children === "Bóng đá"
                ? 1
                : 0,
          )}
      </div>
      <h1 className="mt-4 text-4xl font-black tracking-[-0.05em] text-slate-950 sm:text-5xl">
        {field.name}
      </h1>
      <p className="mt-4 flex items-start gap-2 text-slate-500">
        <MapPin className="mt-0.5 size-5 shrink-0 text-sky-600" />{" "}
        {formatFieldAddress(field)}
      </p>
      <div className="mt-5 flex flex-wrap items-center gap-4 text-sm">
        <span className="inline-flex items-center gap-1.5 font-black text-slate-800">
          <Star className="size-5 fill-amber-400 text-amber-400" />{" "}
          {Number(field.ratingAverage ?? 0).toFixed(1)}
        </span>
        <span className="text-slate-400">
          {field.totalReviews ?? 0} đánh giá
        </span>
      </div>
      {subFields ? (
        <div className="mt-6 rounded-2xl border border-slate-200 bg-white p-5">
          <div className="flex items-center gap-3">
            <span className="grid size-10 place-items-center rounded-xl bg-sky-100 text-sky-700">
              <Layers3 className="size-5" />
            </span>
            <div>
              <strong className="block text-lg text-slate-950">
                {activeSubFields.length} sân con
              </strong>
              <span className="text-xs text-slate-500">
                {Object.keys(subtypeCounts).length} loại sân đang hoạt động
              </span>
            </div>
          </div>
          <div className="mt-4 flex flex-wrap gap-2">
            {Object.entries(subtypeCounts).map(([subtype, count]) => (
              <span
                key={subtype}
                className="rounded-full bg-slate-100 px-3 py-1.5 text-xs font-bold text-slate-600"
              >
                {formatEnum(subtype)} · {count} sân
              </span>
            ))}
          </div>
        </div>
      ) : null}
      <section className="mt-9 border-t border-slate-200 pt-8">
        <h2 className="text-2xl font-black text-slate-950">Giới thiệu</h2>
        <p className="mt-4 whitespace-pre-line leading-7 text-slate-600">
          {field.description ||
            "Chủ sân chưa cập nhật phần giới thiệu cho địa điểm này."}
        </p>
      </section>
    </>
  );
}

function FieldDetailError() {
  return (
    <div className="mx-auto min-h-[60vh] w-full max-w-[90rem] px-5 py-16 sm:px-8">
      <div className="rounded-[2rem] border border-amber-200 bg-amber-50 p-8 text-amber-950">
        <CircleAlert className="size-7" />
        <h1 className="mt-4 text-xl font-black">
          Không thể tải thông tin sân
        </h1>
        <p className="mt-2 text-sm">Vui lòng thử lại sau.</p>
      </div>
    </div>
  );
}

function FieldDetailSkeleton() {
  return (
    <div className="mx-auto min-h-[70vh] w-full max-w-[90rem] animate-pulse px-5 py-8 sm:px-8">
      <div className="aspect-[16/7] rounded-[2rem] bg-slate-200" />
      <div className="mt-8 h-12 w-2/3 rounded-xl bg-slate-200" />
    </div>
  );
}
