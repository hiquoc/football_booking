import { NextResponse } from "next/server";
import { withdrawCommunityApplication } from "@/lib/server/community";
import { assertSameOrigin, routeError } from "@/lib/server/route-response";

export async function PATCH(request: Request, { params }: { params: Promise<{ id: string }> }) {
  try {
    assertSameOrigin(request);
    const { id } = await params;
    return NextResponse.json(await withdrawCommunityApplication(id));
  } catch (error) {
    return routeError(error);
  }
}
