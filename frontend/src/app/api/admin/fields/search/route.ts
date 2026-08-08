import { NextResponse } from "next/server";
import { searchAdminFields } from "@/lib/server/fields";
import { routeError } from "@/lib/server/route-response";

export async function GET(request: Request) {
  try {
    const keyword = new URL(request.url).searchParams.get("keyword")?.trim() ?? "";
    return NextResponse.json(await searchAdminFields(keyword));
  } catch (error) {
    return routeError(error);
  }
}
