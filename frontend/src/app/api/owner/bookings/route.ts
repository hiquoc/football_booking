import { NextResponse } from "next/server";
import { getOwnerBookings } from "@/lib/server/bookings";
import { routeError } from "@/lib/server/route-response";

export async function GET(request: Request) {
  try {
    const query = new URL(request.url).searchParams;
    return NextResponse.json(
      await getOwnerBookings(
        Math.max(0, Number(query.get("page")) || 0),
        Math.min(30, Math.max(1, Number(query.get("size")) || 10)),
        {
          bookingDate: query.get("bookingDate") ?? undefined,
          subFieldId: query.get("subFieldId") ?? undefined,
          status: query.get("status") ?? undefined,
        },
      ),
    );
  } catch (error) {
    return routeError(error);
  }
}
