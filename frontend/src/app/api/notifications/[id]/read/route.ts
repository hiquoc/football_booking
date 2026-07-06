import { NextResponse } from "next/server";
import { markNotificationRead } from "@/lib/server/notifications";
import { assertSameOrigin, routeError } from "@/lib/server/route-response";

export async function PATCH(
  request: Request,
  { params }: { params: Promise<{ id: string }> },
) {
  try {
    assertSameOrigin(request);
    return NextResponse.json(await markNotificationRead((await params).id));
  } catch (error) {
    return routeError(error);
  }
}
