import { NextResponse } from "next/server";
import { getAdminRecurringBookings } from "@/lib/server/recurring-bookings";
import { routeError } from "@/lib/server/route-response";
import type { RecurringBookingStatus } from "@/lib/api/types";

export async function GET(request: Request) {
  try {
    const query = new URL(request.url).searchParams;
    const page = Math.max(0, Number(query.get("page")) || 0);
    const size = Math.min(30, Math.max(1, Number(query.get("size")) || 10));
    const status = (query.get("status") || undefined) as RecurringBookingStatus | undefined;
    return NextResponse.json(await getAdminRecurringBookings(page, size, status));
  } catch (error) {
    return routeError(error);
  }
}
