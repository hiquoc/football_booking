import { NextResponse } from "next/server";
import { getFieldViolations } from "@/lib/server/moderation";
import { errorJson, routeError } from "@/lib/server/route-response";

export async function GET(request: Request) {
  try {
    const query = new URL(request.url).searchParams;
    const fieldId = query.get("fieldId");
    if (!fieldId) return errorJson(400, "VALIDATION_ERROR", "Validation failed.");
    return NextResponse.json(await getFieldViolations(fieldId, Number(query.get("page")) || 0, Number(query.get("size")) || 20));
  } catch (error) {
    return routeError(error);
  }
}
