import { NextResponse } from "next/server";
import { z } from "zod";
import { updateUserStatus } from "@/lib/server/users";
import { assertSameOrigin, routeError } from "@/lib/server/route-response";

const schema = z.object({
  status: z.enum(["ACTIVE", "PLATFORM_BANNED"]),
});

export async function PATCH(
  request: Request,
  { params }: { params: Promise<{ id: string }> },
) {
  try {
    assertSameOrigin(request);
    const input = schema.parse(await request.json());
    return NextResponse.json(await updateUserStatus((await params).id, input.status));
  } catch (error) {
    return routeError(error);
  }
}
