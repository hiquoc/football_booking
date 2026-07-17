import { NextResponse } from "next/server";
import { z } from "zod";
import { getCommunityPost, updateCommunityPost } from "@/lib/server/community";
import { assertSameOrigin, routeError } from "@/lib/server/route-response";

const updateSchema = z.object({
  title: z.string().trim().min(1).max(120),
  description: z.string().trim().max(2000).optional(),
  skillLevel: z.string().trim().min(1).max(40),
  contactPhone: z.string().trim().regex(/^[0-9+]{9,15}$/),
  playersNeeded: z.number().int().positive().optional(),
});

export async function GET(_request: Request, { params }: { params: Promise<{ id: string }> }) {
  try {
    const { id } = await params;
    return NextResponse.json(await getCommunityPost(id));
  } catch (error) {
    return routeError(error);
  }
}

export async function PUT(request: Request, { params }: { params: Promise<{ id: string }> }) {
  try {
    assertSameOrigin(request);
    const { id } = await params;
    return NextResponse.json(await updateCommunityPost(id, updateSchema.parse(await request.json())));
  } catch (error) {
    return routeError(error);
  }
}
