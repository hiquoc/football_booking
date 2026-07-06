import { NextResponse } from "next/server";
import { createFieldReview, getFieldReviews } from "@/lib/server/fields";
import { assertSameOrigin, routeError } from "@/lib/server/route-response";

export async function GET(
  _request: Request,
  { params }: { params: Promise<{ id: string }> },
) {
  try {
    return NextResponse.json(await getFieldReviews((await params).id));
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
    if (!Number.isInteger(body.rating) || body.rating < 1 || body.rating > 5)
      return NextResponse.json(
        { message: "Điểm đánh giá phải từ 1 đến 5" },
        { status: 400 },
      );
    return NextResponse.json(
      await createFieldReview((await params).id, body.rating, body.comment),
    );
  } catch (error) {
    return routeError(error);
  }
}
