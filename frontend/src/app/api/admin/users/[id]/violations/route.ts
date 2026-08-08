import { NextResponse } from "next/server";
import { getUserCommunityViolations } from "@/lib/server/community";
import { getUserFieldViolations, getUserModerationAuditLogs } from "@/lib/server/moderation";
import { routeError } from "@/lib/server/route-response";

export async function GET(
  request: Request,
  { params }: { params: Promise<{ id: string }> },
) {
  try {
    const query = new URL(request.url).searchParams;
    const page = Math.max(0, Number(query.get("page")) || 0);
    const size = Math.min(10, Math.max(1, Number(query.get("size")) || 5));
    const userId = (await params).id;
    const [community, field, audit] = await Promise.all([
      getUserCommunityViolations(userId, page, size),
      getUserFieldViolations(userId, page, size),
      getUserModerationAuditLogs(userId, page, size),
    ]);
    return NextResponse.json({ community, field, audit });
  } catch (error) {
    return routeError(error);
  }
}
