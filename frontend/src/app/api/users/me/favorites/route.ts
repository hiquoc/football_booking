import { NextResponse } from "next/server";
import { getFavoriteFields } from "@/lib/server/fields";
import { routeError } from "@/lib/server/route-response";

export async function GET(request: Request) {
  try {
    const { searchParams } = new URL(request.url);
    const page = Number(searchParams.get("page") ?? 0);
    const size = Number(searchParams.get("size") ?? 4);
    return NextResponse.json(await getFavoriteFields(page, size));
  } catch (error) {
    return routeError(error);
  }
}
