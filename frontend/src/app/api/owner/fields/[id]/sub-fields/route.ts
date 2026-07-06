import { NextResponse } from "next/server";
import { createSubField } from "@/lib/server/fields";
import { assertSameOrigin, routeError } from "@/lib/server/route-response";
export async function POST(
  request: Request,
  { params }: { params: Promise<{ id: string }> },
) {
  try {
    assertSameOrigin(request);
    return NextResponse.json(
      await createSubField((await params).id, await request.json()),
    );
  } catch (error) {
    return routeError(error);
  }
}
