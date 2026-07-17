import { NextResponse } from "next/server";
import { z } from "zod";
import { requestTeamPhotoUploadSlot } from "@/lib/server/users";
import { assertSameOrigin, routeError } from "@/lib/server/route-response";

export async function POST(request: Request) {
  try {
    assertSameOrigin(request);
    const body = z.object({ requestId: z.string().uuid() }).parse(await request.json());
    return NextResponse.json(await requestTeamPhotoUploadSlot(body.requestId));
  } catch (error) {
    return routeError(error);
  }
}
