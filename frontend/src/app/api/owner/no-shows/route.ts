import { NextResponse } from "next/server";
import { z } from "zod";
import { reportNoShow } from "@/lib/server/moderation";
import { assertSameOrigin, routeError } from "@/lib/server/route-response";

const schema = z.object({
  bookingId: z.string().uuid(),
});

export async function POST(request: Request) {
  try {
    assertSameOrigin(request);
    const input = schema.parse(await request.json());
    return NextResponse.json(await reportNoShow(input.bookingId));
  } catch (error) {
    return routeError(error);
  }
}
