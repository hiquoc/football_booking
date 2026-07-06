import { NextResponse } from "next/server";
import { cancelBooking } from "@/lib/server/bookings";
import { assertSameOrigin, routeError } from "@/lib/server/route-response";

export async function PATCH(
  request: Request,
  { params }: { params: Promise<{ id: string }> },
) {
  try {
    assertSameOrigin(request);
    const body = (await request.json().catch(() => ({}))) as {
      reason?: string;
    };
    return NextResponse.json(
      await cancelBooking((await params).id, body.reason),
    );
  } catch (error) {
    return routeError(error);
  }
}
