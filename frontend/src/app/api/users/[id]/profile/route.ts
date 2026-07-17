import { NextResponse } from "next/server";
import { getPublicProfile } from "@/lib/server/users";
import { routeError } from "@/lib/server/route-response";

export async function GET(
  _request: Request,
  { params }: { params: Promise<{ id: string }> },
) {
  try {
    const { id } = await params;
    return NextResponse.json(await getPublicProfile(id));
  } catch (error) {
    return routeError(error);
  }
}
