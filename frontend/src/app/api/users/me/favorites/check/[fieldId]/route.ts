import { NextResponse } from "next/server";
import { checkFavoriteField } from "@/lib/server/fields";
import { routeError } from "@/lib/server/route-response";

export async function GET(
  _request: Request,
  { params }: { params: Promise<{ fieldId: string }> },
) {
  try {
    return NextResponse.json(await checkFavoriteField((await params).fieldId));
  } catch (error) {
    return routeError(error);
  }
}
