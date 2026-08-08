import { NextResponse } from "next/server";
import { createFieldReview, getFieldReviews } from "@/lib/server/fields";
import { assertSameOrigin, errorJson, routeError } from "@/lib/server/route-response";

export async function GET(
  request: Request,
  { params }: { params: Promise<{ id: string }> },
) {
  try {
    const url = new URL(request.url);
    const pageValue = Number(url.searchParams.get("page") ?? "0");
    const sizeValue = Number(url.searchParams.get("size") ?? "6");
    const page = Number.isFinite(pageValue) ? Math.max(0, Math.floor(pageValue)) : 0;
    const size = Number.isFinite(sizeValue) ? Math.max(1, Math.min(Math.floor(sizeValue), 20)) : 6;
    return NextResponse.json(await getFieldReviews((await params).id, page, size));
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
    const body = (await request.json()) as { rating: number; comment?: string };
    if (!Number.isInteger(body.rating) || body.rating < 1 || body.rating > 5) {
      return errorJson(400, "VALIDATION_ERROR", "Rating must be between 1 and 5.");
    }
    return NextResponse.json(
      await createFieldReview((await params).id, body.rating, body.comment),
    );
  } catch (error) {
    return routeError(error);
  }
}
