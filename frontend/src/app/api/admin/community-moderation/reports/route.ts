import { NextResponse } from "next/server";
import { getCommunityReports } from "@/lib/server/community";
import { routeError } from "@/lib/server/route-response";

export async function GET(request: Request) {
  try {
    const query = new URL(request.url).searchParams;
    const page = Math.max(0, Number(query.get("page")) || 0);
    const size = Math.min(50, Math.max(1, Number(query.get("size")) || 20));
    const status = query.get("status") as "PENDING" | "REVIEWED" | null;
    return NextResponse.json(await getCommunityReports(page, size, status ?? undefined));
  } catch (error) {
    return routeError(error);
  }
}
