import { NextResponse } from "next/server";
import { z } from "zod";
import { reportCommunityPost } from "@/lib/server/community";
import { assertSameOrigin, routeError } from "@/lib/server/route-response";

const schema = z.object({
  reason: z.enum(["SPAM", "INAPPROPRIATE_CONTENT", "HARASSMENT", "FAKE_INFORMATION", "SCAM", "OTHER"]),
  description: z.string().trim().max(1000).optional(),
});

export async function POST(request: Request, { params }: { params: Promise<{ id: string }> }) {
  try {
    assertSameOrigin(request);
    const { id } = await params;
    const input = schema.parse(await request.json());
    return NextResponse.json(await reportCommunityPost(id, input.reason, input.description), { status: 201 });
  } catch (error) {
    return routeError(error);
  }
}
