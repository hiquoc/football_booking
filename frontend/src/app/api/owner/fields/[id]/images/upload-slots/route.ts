import { NextResponse } from "next/server";
import { requestFieldImageUploadSlots } from "@/lib/server/fields";
import { assertSameOrigin, errorJson, routeError } from "@/lib/server/route-response";

export async function POST(
  request: Request,
  { params }: { params: Promise<{ id: string }> },
) {
  try {
    assertSameOrigin(request);
    const body = (await request.json()) as { requestId?: string; count?: number };
    if (!body.requestId || !Number.isInteger(body.count) || body.count! < 1 || body.count! > 10) {
      return errorJson(400, "VALIDATION_ERROR", "Validation failed.");
    }
    return NextResponse.json(
      await requestFieldImageUploadSlots((await params).id, body.requestId, body.count!),
    );
  } catch (error) {
    return routeError(error);
  }
}
