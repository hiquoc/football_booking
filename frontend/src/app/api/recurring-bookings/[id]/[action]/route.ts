import { NextResponse } from "next/server";
import { changeRecurringBookingStatus } from "@/lib/server/recurring-bookings";
import { assertSameOrigin, errorJson, routeError } from "@/lib/server/route-response";

type Action = "pause" | "resume" | "cancel";

async function handle(request: Request, paramsPromise: Promise<{ id: string; action: string }>) {
  assertSameOrigin(request);
  const { id, action } = await paramsPromise;
  if (!["pause", "resume", "cancel"].includes(action)) {
    return errorJson(400, "INVALID_REQUEST", "Invalid request.");
  }
  const query = new URL(request.url).searchParams;
  const scope = query.get("admin") === "true" ? "admin" : query.get("owner") === "true" ? "owner" : "my";
  return NextResponse.json(await changeRecurringBookingStatus(id, action as Action, scope));
}

export async function PATCH(request: Request, { params }: { params: Promise<{ id: string; action: string }> }) {
  try {
    return await handle(request, params);
  } catch (error) {
    return routeError(error);
  }
}

export async function DELETE(request: Request, { params }: { params: Promise<{ id: string; action: string }> }) {
  try {
    return await handle(request, params);
  } catch (error) {
    return routeError(error);
  }
}
