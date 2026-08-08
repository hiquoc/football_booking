import { NextResponse } from "next/server";
import { getAvailability } from "@/lib/server/bookings";
import { errorJson, routeError } from "@/lib/server/route-response";

export async function GET(request: Request) {
  try {
    const query = new URL(request.url).searchParams;
    const subFieldId = query.get("subFieldId");
    const date = query.get("date");
    if (!subFieldId || !date) {
      return errorJson(400, "VALIDATION_ERROR", "Missing sub-field or booking date.");
    }
    return NextResponse.json(await getAvailability(subFieldId, date));
  } catch (error) {
    return routeError(error);
  }
}
