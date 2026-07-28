import { NextResponse } from "next/server";
import { getSubFieldFilterOptions } from "@/lib/server/fields";
import { routeError } from "@/lib/server/route-response";

export async function GET(request: Request) {
  try {
    const search = new URL(request.url).searchParams.get("search") ?? undefined;
    return NextResponse.json(await getSubFieldFilterOptions(search));
  } catch (error) {
    return routeError(error);
  }
}
