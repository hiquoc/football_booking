import { NextResponse } from "next/server";
import { createSubField, getSubFields } from "@/lib/server/fields";
import { assertSameOrigin, routeError } from "@/lib/server/route-response";

export async function GET(
  _request: Request,
  { params }: { params: Promise<{ id: string }> },
) {
  try {
    return NextResponse.json(await getSubFields((await params).id));
  } catch (error) {
    return routeError(error);
  }
}

export async function POST(
  request: Request,
  { params }: { params: Promise<{ id: string }> },
) {
  try {
    assertSameOrigin(request);
    return NextResponse.json(
      await createSubField((await params).id, await request.json()),
    );
  } catch (error) {
    return routeError(error);
  }
}
