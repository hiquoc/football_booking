import { NextResponse } from "next/server";
import type { CloudinaryUploadResult } from "@/lib/api/types";
import { confirmFieldImageUploads } from "@/lib/server/fields";
import { assertSameOrigin, routeError } from "@/lib/server/route-response";

export async function POST(request: Request, { params }: { params: Promise<{ id: string }> }) {
  try {
    assertSameOrigin(request);
    const results = (await request.json()) as CloudinaryUploadResult[];
    return NextResponse.json(await confirmFieldImageUploads((await params).id, results));
  } catch (error) { return routeError(error); }
}
