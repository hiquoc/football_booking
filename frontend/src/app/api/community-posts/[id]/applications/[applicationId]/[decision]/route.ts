import { NextResponse } from "next/server";
import { decideCommunityApplication } from "@/lib/server/community";
import { assertSameOrigin, errorJson, routeError } from "@/lib/server/route-response";

export async function PATCH(
  request: Request,
  { params }: { params: Promise<{ id: string; applicationId: string; decision: string }> },
) {
  try {
    assertSameOrigin(request);
    const { id, applicationId, decision } = await params;
    if (decision !== "accept" && decision !== "reject") {
      return errorJson(404, "RESOURCE_NOT_FOUND", "Resource not found.");
    }
    return NextResponse.json(await decideCommunityApplication(id, applicationId, decision));
  } catch (error) {
    return routeError(error);
  }
}
