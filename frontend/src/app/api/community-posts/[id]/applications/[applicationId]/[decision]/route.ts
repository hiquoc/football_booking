import { NextResponse } from "next/server";
import { decideCommunityApplication } from "@/lib/server/community";
import { assertSameOrigin, routeError } from "@/lib/server/route-response";

export async function PATCH(
  request: Request,
  { params }: { params: Promise<{ id: string; applicationId: string; decision: string }> },
) {
  try {
    assertSameOrigin(request);
    const { id, applicationId, decision } = await params;
    if (decision !== "accept" && decision !== "reject") {
      return NextResponse.json({ message: "Invalid decision" }, { status: 404 });
    }
    return NextResponse.json(await decideCommunityApplication(id, applicationId, decision));
  } catch (error) {
    return routeError(error);
  }
}
