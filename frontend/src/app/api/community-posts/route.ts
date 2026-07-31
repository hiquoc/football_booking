import { NextResponse } from "next/server";
import { z } from "zod";
import { createCommunityPost, getCommunityPosts } from "@/lib/server/community";
import { uuidSchema } from "@/lib/api/common-schemas";
import { assertSameOrigin, routeError } from "@/lib/server/route-response";

const postTypeSchema = z.enum(["LOOKING_OPPONENT", "LOOKING_PLAYER"]);
const skillSchema = z.string().trim().min(1).max(40);

const createSchema = z.object({
  bookingId: uuidSchema,
  postType: postTypeSchema,
  title: z.string().trim().min(1).max(120),
  description: z.string().trim().max(2000).optional(),
  skillLevel: skillSchema,
  contactPhone: z.string().trim().regex(/^[0-9+]{9,15}$/),
  playersNeeded: z.number().int().positive().optional(),
  ownerDisplayName: z.string().trim().max(255).nullable().optional(),
  ownerAvatarUrl: z.string().trim().max(1000).nullable().optional(),
  ownerTeamPhotoUrl: z.string().trim().max(1000).nullable().optional(),
});

export async function GET(request: Request) {
  try {
    const query = new URL(request.url).searchParams;
    const page = Math.max(0, Number(query.get("page")) || 0);
    const size = Math.min(30, Math.max(1, Number(query.get("size")) || 10));
    const filters = Object.fromEntries(
      ["ownerId", "applicantId", "postType", "skillLevel", "date", "fieldType", "city", "district", "fieldName", "status", "keyword", "sortBy"]
        .map((key) => [key, query.get(key) || undefined])
        .filter(([key, value]) => value && !(key === "status" && value === "all")),
    );
    return NextResponse.json(await getCommunityPosts(page, size, filters));
  } catch (error) {
    return routeError(error);
  }
}

export async function POST(request: Request) {
  try {
    assertSameOrigin(request);
    return NextResponse.json(await createCommunityPost(createSchema.parse(await request.json())), { status: 201 });
  } catch (error) {
    return routeError(error);
  }
}
