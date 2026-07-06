import { NextResponse } from "next/server";
import { z } from "zod";
import { getMyProfile, updateMyProfile } from "@/lib/server/users";
import { assertSameOrigin, routeError } from "@/lib/server/route-response";

const profileSchema = z.object({
  fullName: z.string().trim().min(2).max(100).optional(),
});

export async function GET() {
  try {
    return NextResponse.json(await getMyProfile());
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
