import { NextResponse } from "next/server";
import { getUnreadCount } from "@/lib/server/notifications";
import { routeError } from "@/lib/server/route-response";

export async function GET() {
  try {
    return NextResponse.json(await getUnreadCount());
  } catch (error) {
    return routeError(error);
  }
}
