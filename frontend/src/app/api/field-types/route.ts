import { NextResponse } from "next/server";
import { fieldTypeSchema } from "@/lib/api/field-type-schema";
import { createFieldType, getFieldTypes } from "@/lib/server/field-types";
import { assertSameOrigin, routeError } from "@/lib/server/route-response";

export async function GET() {
  try {
    return NextResponse.json(await getFieldTypes());
  } catch (error) {
    return routeError(error);
  }
}
export async function POST(request: Request) {
  try {
    assertSameOrigin(request);
    return NextResponse.json(
      await createFieldType(fieldTypeSchema.parse(await request.json())),
    );
  } catch (error) {
    return routeError(error);
  }
}
