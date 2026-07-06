import { NextResponse } from "next/server";
import { deleteClosure, updateClosure } from "@/lib/server/fields";
import { assertSameOrigin, routeError } from "@/lib/server/route-response";
export async function DELETE(
  request: Request,
  { params }: { params: Promise<{ closureId: string }> },
) {
  try {
    assertSameOrigin(request);
    await deleteClosure((await params).closureId);
    return NextResponse.json({ success: true });
  } catch (error) {
    return routeError(error);
  }
}

export async function PUT(
  request: Request,
  { params }: { params: Promise<{ closureId: string }> },
) {
  try {
    assertSameOrigin(request);
    return NextResponse.json(
      await updateClosure((await params).closureId, await request.json()),
    );
  } catch (error) {
    return routeError(error);
  }
}
