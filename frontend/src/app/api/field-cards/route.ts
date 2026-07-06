import { NextResponse } from "next/server";
import { getFieldCards } from "@/lib/server/fields";
import { routeError } from "@/lib/server/route-response";
import type { FieldCardFilters } from "@/lib/api/types";

const filterKeys = [
  "fieldType",
  "subFieldType",
  "district",
  "provinceCode",
  "latitude",
  "longitude",
  "radiusKm",
  "sortBy",
  "direction",
] as const;

export async function GET(request: Request) {
  try {
    const params = new URL(request.url).searchParams;
    const page = Math.max(0, Number.parseInt(params.get("page") ?? "0", 10) || 0);
    const size = Math.min(50, Math.max(1, Number.parseInt(params.get("size") ?? "9", 10) || 9));
    const filters = Object.fromEntries(
      filterKeys.flatMap((key) => {
        const value = params.get(key)?.trim();
        return value ? [[key, value]] : [];
      }),
    ) as FieldCardFilters;
    return NextResponse.json(await getFieldCards(page, size, filters));
  } catch (error) {
    return routeError(error);
  }
}
