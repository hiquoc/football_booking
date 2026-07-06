import { NextResponse } from "next/server";
import { getNotifications } from "@/lib/server/notifications";
import { routeError } from "@/lib/server/route-response";

export async function GET(request: Request) {
  try {
    const query = new URL(request.url).searchParams;
    return NextResponse.json(
      await getNotifications(
        Math.max(0, Number(query.get("page")) || 0),
        Math.min(50, Math.max(1, Number(query.get("size")) || 20)),
      ),
    );
  } catch (error) {
    return routeError(error);
  }
}
