import { NextResponse } from "next/server";
import { payBooking } from "@/lib/server/bookings";
import { assertSameOrigin, routeError } from "@/lib/server/route-response";

export async function POST(
  request: Request,
  context: { params: Promise<{ id: string }> },
) {
  try {
    assertSameOrigin(request);
    const { id } = await context.params;
    return NextResponse.json(await payBooking(id));
  } catch (error) {
    return routeError(error);
  }
}
