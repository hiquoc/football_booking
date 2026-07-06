import { NextResponse } from "next/server";
import { createNotificationSocketTicket } from "@/lib/server/notifications";
import { assertSameOrigin, routeError } from "@/lib/server/route-response";

export async function POST(request: Request) {
  try {
    assertSameOrigin(request);
    return NextResponse.json(await createNotificationSocketTicket());
  } catch (error) {
    return routeError(error);
  }
}
