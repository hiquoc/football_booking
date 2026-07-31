import { NextResponse } from "next/server";
import { z } from "zod";
import { banClient } from "@/lib/server/moderation";
import { assertSameOrigin, routeError } from "@/lib/server/route-response";

const schema = z.object({ fieldId: z.string().uuid() });

export async function PATCH(request: Request, { params }: { params: Promise<{ userId: string }> }) {
  try {
    assertSameOrigin(request);
    const { userId } = await params;
    const input = schema.parse(await request.json());
    return NextResponse.json(await banClient(input.fieldId, userId));
  } catch (error) {
    return routeError(error);
  }
}
