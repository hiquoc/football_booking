import { NextResponse } from "next/server";
import { getBookingConfig } from "@/lib/server/bookings";
import { routeError } from "@/lib/server/route-response";

export async function GET() {
  try {
    return NextResponse.json(await getBookingConfig());
  } catch (error) {
    return routeError(error);
  }
}
