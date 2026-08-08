import { NextResponse } from "next/server";
import { getModerationAuditLogs } from "@/lib/server/moderation";
import { errorJson, routeError } from "@/lib/server/route-response";

export async function GET(request: Request) {
  try {
    const query = new URL(request.url).searchParams;
    const fieldId = query.get("fieldId")?.trim();
    if (!fieldId) return errorJson(400, "VALIDATION_ERROR", "Validation failed.");
    return NextResponse.json(await getModerationAuditLogs(fieldId, Number(query.get("page")) || 0, Number(query.get("size")) || 20));
  } catch (error) {
    return routeError(error);
  }
}
