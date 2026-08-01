import { NextResponse } from "next/server";
import { getSubFieldTypes } from "@/lib/server/field-types";
import { routeError } from "@/lib/server/route-response";

export async function GET() {
  try {
    return NextResponse.json(await getSubFieldTypes(), {
      headers: {
        "Cache-Control": "public, max-age=31536000, immutable",
      },
    });
  } catch (error) {
    return routeError(error);
  }
}
