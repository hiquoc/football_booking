import { NextResponse } from "next/server";
import { communityPostAction } from "@/lib/server/community";
import { assertSameOrigin, errorJson, routeError } from "@/lib/server/route-response";

export async function PATCH(request: Request, { params }: { params: Promise<{ id: string; action: string }> }) {
  try {
    assertSameOrigin(request);
    const { id, action } = await params;
    if (action !== "close" && action !== "full") {
      return errorJson(404, "RESOURCE_NOT_FOUND", "Resource not found.");
    }
    return NextResponse.json(await communityPostAction(id, action));
  } catch (error) {
    return routeError(error);
  }
}
