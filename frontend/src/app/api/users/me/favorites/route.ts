import { NextResponse } from "next/server";
import { getFavoriteFields } from "@/lib/server/fields";
import { routeError } from "@/lib/server/route-response";

export async function GET() {
  try {
    return NextResponse.json(await getFavoriteFields());
  } catch (error) {
    return routeError(error);
  }
}
