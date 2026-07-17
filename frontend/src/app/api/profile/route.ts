import { NextResponse } from "next/server";
import { z } from "zod";
import { getMyPublicProfile, updateMyProfile } from "@/lib/server/users";
import { assertSameOrigin, routeError } from "@/lib/server/route-response";

const profileSchema = z.object({
  fullName: z.string().trim().min(2).max(100).optional(),
  phoneNumber: z.string().trim().max(20).optional(),
  bio: z.string().trim().max(500).nullable().optional(),
  skillLevel: z.enum([
    "VERY_WEAK",
    "WEAK",
    "AVERAGE",
    "ABOVE_AVERAGE",
    "GOOD",
    "VERY_GOOD",
    "SEMI_PRO",
    "PRO",
  ]).optional(),
});

export async function GET() {
  try {
    return NextResponse.json(await getMyPublicProfile());
  } catch (error) {
    return routeError(error);
  }
}

export async function PATCH(request: Request) {
  try {
    assertSameOrigin(request);
    return NextResponse.json(
      await updateMyProfile(profileSchema.parse(await request.json())),
    );
  } catch (error) {
    return routeError(error);
  }
}
