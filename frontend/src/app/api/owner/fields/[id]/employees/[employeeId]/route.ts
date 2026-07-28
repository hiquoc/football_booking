import { NextResponse } from "next/server";
import { removeFieldEmployee } from "@/lib/server/fields";
import { assertSameOrigin, routeError } from "@/lib/server/route-response";

export async function DELETE(
  request: Request,
  { params }: { params: Promise<{ id: string; employeeId: string }> },
) {
  try {
    assertSameOrigin(request);
    const { id, employeeId } = await params;
    return NextResponse.json(await removeFieldEmployee(id, employeeId));
  } catch (error) {
    return routeError(error);
  }
}
