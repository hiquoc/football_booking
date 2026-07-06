import { NextResponse } from "next/server";
import { confirmMockPayment } from "@/lib/server/bookings";
import { assertSameOrigin, routeError } from "@/lib/server/route-response";

export async function PATCH(
  request: Request,
  { params }: { params: Promise<{ id: string }> },
) {
  try {
    assertSameOrigin(request);
    return NextResponse.json(await confirmMockPayment((await params).id));
  } catch (error) {
    return routeError(error);
  }
}
