import { NextResponse } from "next/server";
import { createField, getOwnerFields } from "@/lib/server/fields";
import { fieldInputSchema } from "@/lib/api/field-schema";
import { normalizeFieldLocation } from "@/lib/server/field-location";
import { assertSameOrigin, routeError } from "@/lib/server/route-response";

export async function GET(request: Request) {
  try {
    const query = new URL(request.url).searchParams;
    return NextResponse.json(
      await getOwnerFields(
        Math.max(0, Number(query.get("page")) || 0),
        Math.min(30, Math.max(1, Number(query.get("size")) || 10)),
      ),
    );
  } catch (error) {
    return routeError(error);
  }
}

export async function POST(request: Request) {
  try {
    assertSameOrigin(request);
    const input = fieldInputSchema.parse(await request.json());
    return NextResponse.json(
      await createField(await normalizeFieldLocation(input)),
    );
  } catch (error) {
    return routeError(error);
  }
}
