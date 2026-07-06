import { NextResponse } from "next/server";
import { getFieldDetails } from "@/lib/server/fields";
import { routeError } from "@/lib/server/route-response";

export async function GET(
  _request: Request,
  { params }: { params: Promise<{ id: string }> },
) {
  try {
    return NextResponse.json(await getFieldDetails((await params).id));
  } catch (error) {
    return routeError(error);
  }
}
