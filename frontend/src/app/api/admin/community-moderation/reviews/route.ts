import { NextResponse } from "next/server";
import { z } from "zod";
import { submitCommunityModeration } from "@/lib/server/community";
import { assertSameOrigin, routeError } from "@/lib/server/route-response";
import { uuidSchema } from "@/lib/api/common-schemas";

const schema = z.object({
  action: z.enum(["NO_ACTION", "HIDE_POST", "RESTORE_POST", "ISSUE_WARNING", "TEMPORARY_POSTING_BAN", "PERMANENT_POSTING_BAN"]),
  targetUserId: uuidSchema.optional(),
  targetPostId: uuidSchema.optional(),
  reason: z.string().trim().min(1).max(500),
  note: z.string().trim().max(1000).optional(),
  expireAt: z.string().datetime().optional(),
});

export async function POST(request: Request) {
  try {
    assertSameOrigin(request);
    return NextResponse.json(await submitCommunityModeration(schema.parse(await request.json())));
  } catch (error) {
    return routeError(error);
  }
}
