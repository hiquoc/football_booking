import { NextResponse } from "next/server";
import type { CloudinaryUploadResult } from "@/lib/api/types";
import { confirmTeamPhotoUpload } from "@/lib/server/users";
import { assertSameOrigin, routeError } from "@/lib/server/route-response";

export async function POST(request: Request) {
  try {
    assertSameOrigin(request);
    return NextResponse.json(await confirmTeamPhotoUpload((await request.json()) as CloudinaryUploadResult));
  } catch (error) {
    return routeError(error);
  }
}
