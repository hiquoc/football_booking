import { NextResponse } from "next/server";
import { getMyProfile } from "@/lib/server/users";
import { routeError } from "@/lib/server/route-response";

export async function GET() {
  try {
    return NextResponse.json(await getMyProfile());
  } catch (error) {
    return routeError(error);
  }
}
