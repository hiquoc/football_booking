import { NextResponse } from "next/server";
import { changeFieldImageOrder } from "@/lib/server/fields";
import { assertSameOrigin, routeError } from "@/lib/server/route-response";
export async function PUT(
  request: Request,
  { params }: { params: Promise<{ id: string }> },
) {
  try {
    assertSameOrigin(request);
    const input = (await request.json()) as {
      imageIds: number[];
    };
    return NextResponse.json(
      await changeFieldImageOrder((await params).id, input.imageIds),
    );
  } catch (error) {
    return routeError(error);
  }
}
