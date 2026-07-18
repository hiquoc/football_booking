import { NextResponse } from "next/server";
import { z } from "zod";
import { createPaymentDispute, getOwnerPaymentDisputes } from "@/lib/server/moderation";
import { assertSameOrigin, routeError } from "@/lib/server/route-response";

const schema = z.object({
  bookingId: z.string().uuid(),
  description: z.string().trim().min(1),
  imageUrls: z.array(z.string().url()).min(1),
});

export async function GET(request: Request) {
  try {
    const query = new URL(request.url).searchParams;
    return NextResponse.json(await getOwnerPaymentDisputes(Number(query.get("page")) || 0, Number(query.get("size")) || 20));
  } catch (error) {
    return routeError(error);
  }
}

export async function POST(request: Request) {
  try {
    assertSameOrigin(request);
    return NextResponse.json(await createPaymentDispute(schema.parse(await request.json())));
  } catch (error) {
    return routeError(error);
  }
}
