import type { Metadata } from "next";
import { dehydrate, HydrationBoundary } from "@tanstack/react-query";
import { MapPinned } from "lucide-react";
import { FieldListContent } from "@/components/fields/field-list-content";
import { prefetchFieldCards } from "@/lib/server/field-query-cache";
import { getCurrentUser } from "@/lib/server/session";
import type { FieldCardFilters } from "@/lib/api/types";

export const metadata: Metadata = {
  title: "Danh sách sân",
  description: "Khám phá các sân thể thao và địa điểm thi đấu phù hợp với bạn.",
};

export default async function FieldsPage({
  searchParams,
}: {
  searchParams: Promise<Record<string, string | string[] | undefined>>;
}) {
  const params = await searchParams;
  const pageNumber = parsePageNumber(single(params.page));
  const filters = parseFilters(params);
  const pageIndex = pageNumber - 1;
  const user = await getCurrentUser();
  const queryClient = await prefetchFieldCards(pageIndex, 9, filters);

  return (
    <div className="min-h-[70vh]">
      <FieldsHero />
      <section className="mx-auto w-full max-w-[90rem] px-5 py-12 sm:px-8 sm:py-16">
        <HydrationBoundary state={dehydrate(queryClient)}>
          <FieldListContent
            pageNumber={pageNumber}
            filters={filters}
            viewerRole={user?.userType ?? null}
          />
        </HydrationBoundary>
      </section>
    </div>
  );
}

function FieldsHero() {
  return (
    <section className="relative overflow-hidden bg-sky-50 py-16 text-slate-900 sm:py-20">
      <div className="absolute inset-0 bg-[linear-gradient(to_right,#80808012_1px,transparent_1px),linear-gradient(to_bottom,#80808012_1px,transparent_1px)] bg-[size:64px_64px] [mask-image:linear-gradient(to_bottom,black,transparent_90%)]" />
      <div className="relative mx-auto w-full max-w-[90rem] px-5 sm:px-8">
        <span className="inline-flex items-center gap-2 text-xs font-black uppercase tracking-[0.18em] text-sky-600">
          <MapPinned className="size-4" /> Khám phá địa điểm
        </span>
        <h1 className="mt-4 text-4xl font-black tracking-[-0.05em] sm:text-6xl text-slate-950">
          Tìm sân phù hợp với bạn.
        </h1>
        <p className="mt-4 max-w-2xl leading-7 text-slate-600">
          Xem thông tin, khung giờ hoạt động và đánh giá thực tế trước khi chọn
          sân.
        </p>
      </div>
    </section>
  );
}

function parsePageNumber(value?: string) {
  const parsed = Number.parseInt(value ?? "1", 10);
  return Number.isFinite(parsed) && parsed > 0 ? parsed : 1;
}

function single(value: string | string[] | undefined) {
  return Array.isArray(value) ? value[0] : value;
}

function parseFilters(
  params: Record<string, string | string[] | undefined>,
): FieldCardFilters {
  const value = (key: string) => single(params[key])?.trim() || undefined;
  const sortBy = value("sortBy");
  const direction = value("direction");
  return {
    keyword: value("keyword"),
    fieldType: value("fieldType"),
    subFieldType: value("subFieldType"),
    district: value("district"),
    provinceCode: value("provinceCode"),
    latitude: value("latitude"),
    longitude: value("longitude"),
    radiusKm: value("radiusKm"),
    sortBy: ["rating", "reviews", "newest", "distance"].includes(sortBy ?? "")
      ? (sortBy as FieldCardFilters["sortBy"])
      : undefined,
    direction: direction === "asc" || direction === "desc" ? direction : undefined,
  };
}
