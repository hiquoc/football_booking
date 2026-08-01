import { NextResponse } from "next/server";
import { createClosures, getSubFieldClosures } from "@/lib/server/fields";
import { assertSameOrigin, errorJson, routeError } from "@/lib/server/route-response";

export async function GET(request: Request) {
  try {
    const id = new URL(request.url).searchParams.get("subFieldId");
    if (!id) return errorJson(400, "VALIDATION_ERROR", "Validation failed.");
    return NextResponse.json(await getSubFieldClosures(id));
  } catch (error) {
    return routeError(error);
  }
}

export async function POST(request: Request) {
  try {
    assertSameOrigin(request);
    const input = (await request.json()) as {
      subFieldIds: string[];
      startDate: string;
      endDate: string;
      reason: string;
    };
    return NextResponse.json(
      await createClosures(
        input.subFieldIds,
        input.startDate,
        input.endDate,
        input.reason,
      ),
    );
  } catch (error) {
    return routeError(error);
  }
}
