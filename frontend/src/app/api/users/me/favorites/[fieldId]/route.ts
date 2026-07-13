import { NextResponse } from "next/server";
import {
  addFavoriteField,
  removeFavoriteField,
} from "@/lib/server/fields";
import { assertSameOrigin, routeError } from "@/lib/server/route-response";

export async function POST(
  request: Request,
  { params }: { params: Promise<{ fieldId: string }> },
) {
  try {
    assertSameOrigin(request);
    return NextResponse.json(await addFavoriteField((await params).fieldId));
  } catch (error) {
    return routeError(error);
  }
}

export async function DELETE(
  request: Request,
  { params }: { params: Promise<{ fieldId: string }> },
) {
  try {
    assertSameOrigin(request);
    await removeFavoriteField((await params).fieldId);
    return NextResponse.json(null);
  } catch (error) {
    return routeError(error);
  }
}
