import { NextResponse } from "next/server";
import { getUsers } from "@/lib/server/users";
import { routeError } from "@/lib/server/route-response";

export async function GET(request: Request) {
  try {
    const query = new URL(request.url).searchParams;
    const page = Math.max(0, Number.parseInt(query.get("page") ?? "0", 10) || 0);
    const size = Math.min(
      30,
      Math.max(1, Number.parseInt(query.get("size") ?? "10", 10) || 10),
    );
    return NextResponse.json(await getUsers(page, size));
  } catch (error) {
    return routeError(error);
  }
}
