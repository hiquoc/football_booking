import { NextResponse } from "next/server";
import { communityPostAction } from "@/lib/server/community";
import { assertSameOrigin, routeError } from "@/lib/server/route-response";

export async function PATCH(request: Request, { params }: { params: Promise<{ id: string; action: string }> }) {
  try {
    assertSameOrigin(request);
    const { id, action } = await params;
    if (action !== "close" && action !== "full") {
      return NextResponse.json({ message: "Invalid action" }, { status: 404 });
    }
    return NextResponse.json(await communityPostAction(id, action));
  } catch (error) {
    return routeError(error);
  }
}
