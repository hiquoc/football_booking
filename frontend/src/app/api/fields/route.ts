import { NextResponse } from "next/server";
import { getFields } from "@/lib/server/fields";
import { routeError } from "@/lib/server/route-response";
import type { FieldStatus } from "@/lib/api/types";

const fieldStatuses = new Set<FieldStatus>([
  "PENDING",
  "APPROVED",
  "REJECTED",
]);

export async function GET(request: Request) {
  try {
    const searchParams = new URL(request.url).searchParams;
    const page = Math.max(
      0,
      Number.parseInt(searchParams.get("page") ?? "0", 10) || 0,
    );
    const size = Math.min(
      24,
      Math.max(1, Number.parseInt(searchParams.get("size") ?? "9", 10) || 9),
    );
    const requestedStatus = searchParams.get("status");
    const status = fieldStatuses.has(requestedStatus as FieldStatus)
      ? (requestedStatus as FieldStatus)
      : undefined;
    return NextResponse.json(await getFields(page, size, status));
  } catch (error) {
    return routeError(error);
  }
}
