import { NextResponse } from "next/server";
import { getOwnerBookings } from "@/lib/server/bookings";
import { routeError } from "@/lib/server/route-response";

function statusFilter(value: string | null) {
  const status = value?.trim();
  return status && status !== "ALL" ? status : undefined;
}

export async function GET(request: Request) {
  try {
    const query = new URL(request.url).searchParams;
    return NextResponse.json(
      await getOwnerBookings(
        Math.max(0, Number(query.get("page")) || 0),
        Math.min(30, Math.max(1, Number(query.get("size")) || 10)),
        {
          bookingDate: query.get("bookingDate")?.trim() || undefined,
          fieldId: query.get("fieldId")?.trim() || undefined,
          fieldType: query.get("fieldType")?.trim() || undefined,
          subFieldType: query.get("subFieldType")?.trim() || undefined,
          status: statusFilter(query.get("status")),
        },
      ),
    );
  } catch (error) {
    return routeError(error);
  }
}
