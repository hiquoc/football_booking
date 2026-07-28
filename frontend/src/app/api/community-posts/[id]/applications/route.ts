import { NextResponse } from "next/server";
import { z } from "zod";
import { applyCommunityPost } from "@/lib/server/community";
import { assertSameOrigin, routeError } from "@/lib/server/route-response";

const optionalString = (max: number) => z.string().trim().max(max).nullish().transform((value) => value ?? undefined);

const schema = z.object({
  message: optionalString(1000),
  applicantDisplayName: optionalString(255),
  applicantAvatarUrl: optionalString(1000),
  applicantTeamPhotoUrl: optionalString(1000),
  applicantSkillLevel: optionalString(40),
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
