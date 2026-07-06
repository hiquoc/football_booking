import { NextResponse } from "next/server";
import { deleteFieldImage } from "@/lib/server/fields";
import { assertSameOrigin, routeError } from "@/lib/server/route-response";
export async function DELETE(
  request: Request,
  { params }: { params: Promise<{ id: string; imageId: string }> },
) {
  try {
    assertSameOrigin(request);
    const values = await params;
    await deleteFieldImage(values.id, Number(values.imageId));
    return NextResponse.json({ success: true });
  } catch (error) {
    return routeError(error);
  }
}
