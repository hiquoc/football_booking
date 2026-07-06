import { NextResponse } from "next/server";
import { getBooking } from "@/lib/server/bookings";
import { routeError } from "@/lib/server/route-response";

export async function GET(
  _request: Request,
  { params }: { params: Promise<{ id: string }> },
) {
  try {
    return NextResponse.json(await getBooking((await params).id));
  } catch (error) {
    return routeError(error);
  }
}
