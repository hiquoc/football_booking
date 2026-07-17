import { NextResponse } from "next/server";
import { z } from "zod";
import { applyCommunityPost } from "@/lib/server/community";
import { assertSameOrigin, routeError } from "@/lib/server/route-response";

const schema = z.object({
  message: z.string().trim().max(1000).optional(),
  applicantDisplayName: z.string().trim().max(255).optional(),
  applicantAvatarUrl: z.string().trim().max(1000).optional(),
  applicantTeamPhotoUrl: z.string().trim().max(1000).optional(),
  applicantSkillLevel: z.string().trim().max(40).optional(),
});

export async function POST(request: Request, { params }: { params: Promise<{ id: string }> }) {
  try {
    assertSameOrigin(request);
    const { id } = await params;
    return NextResponse.json(await applyCommunityPost(id, schema.parse(await request.json())), { status: 201 });
  } catch (error) {
    return routeError(error);
  }
}
