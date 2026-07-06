import { NextResponse } from "next/server";
import { fieldTypeSchema } from "@/lib/api/field-type-schema";
import { deleteFieldType, updateFieldType } from "@/lib/server/field-types";
import { assertSameOrigin, routeError } from "@/lib/server/route-response";

export async function PUT(
  request: Request,
  { params }: { params: Promise<{ id: string }> },
) {
  try {
    assertSameOrigin(request);
    return NextResponse.json(
      await updateFieldType(
        Number((await params).id),
        fieldTypeSchema.parse(await request.json()),
      ),
    );
  } catch (error) {
    return routeError(error);
  }
}
export async function DELETE(
  request: Request,
  { params }: { params: Promise<{ id: string }> },
) {
  try {
    assertSameOrigin(request);
    await deleteFieldType(Number((await params).id));
    return NextResponse.json({ success: true });
  } catch (error) {
    return routeError(error);
  }
}
