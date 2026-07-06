import { NextResponse } from "next/server";
import { deleteSubField, updateSubField } from "@/lib/server/fields";
import { assertSameOrigin, routeError } from "@/lib/server/route-response";
export async function DELETE(
  request: Request,
  { params }: { params: Promise<{ id: string; subFieldId: string }> },
) {
  try {
    assertSameOrigin(request);
    const { id, subFieldId } = await params;
    await deleteSubField(id, subFieldId);
    return NextResponse.json({ success: true });
  } catch (error) {
    return routeError(error);
  }
}

export async function PUT(
  request: Request,
  { params }: { params: Promise<{ id: string; subFieldId: string }> },
) {
  try {
    assertSameOrigin(request);
    const { id, subFieldId } = await params;
    return NextResponse.json(await updateSubField(id, subFieldId, await request.json()));
  } catch (error) {
    return routeError(error);
  }
}
