import { NextResponse } from "next/server";
import { updateField } from "@/lib/server/fields";
import { fieldInputSchema } from "@/lib/api/field-schema";
import { normalizeFieldLocation } from "@/lib/server/field-location";
import { assertSameOrigin, routeError } from "@/lib/server/route-response";
export async function PUT(
  request: Request,
  { params }: { params: Promise<{ id: string }> },
) {
  try {
    assertSameOrigin(request);
    const input = fieldInputSchema.parse(await request.json());
    return NextResponse.json(
      await updateField(
        (await params).id,
        await normalizeFieldLocation(input),
      ),
    );
  } catch (error) {
    return routeError(error);
  }
}
