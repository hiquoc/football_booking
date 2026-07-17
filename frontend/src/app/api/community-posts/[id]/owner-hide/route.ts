import { NextResponse } from "next/server";
import { z } from "zod";
import { ownerHideCommunityPost } from "@/lib/server/community";
import { assertSameOrigin, routeError } from "@/lib/server/route-response";

const schema = z.object({ reason: z.string().trim().min(1).max(500) });

export async function PATCH(request: Request, { params }: { params: Promise<{ id: string }> }) {
  try {
    assertSameOrigin(request);
    const { id } = await params;
    const { reason } = schema.parse(await request.json());
    return NextResponse.json(await ownerHideCommunityPost(id, reason));
  } catch (error) {
    return routeError(error);
  }
}
