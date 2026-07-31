import { NextResponse } from "next/server";
import { z } from "zod";
import { createBooking, getMyBookings } from "@/lib/server/bookings";
import { assertSameOrigin, routeError } from "@/lib/server/route-response";
import { uuidSchema } from "@/lib/api/common-schemas";

const bookingSchema = z.object({
  subFieldId: uuidSchema,
  bookingDate: z.string().date().optional(),
  startTime: z.string().regex(/^\d{2}:\d{2}(:\d{2})?$/).optional(),
  startDateTime: z.string().regex(/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}(:\d{2})?$/),
  endDateTime: z.string().regex(/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}(:\d{2})?$/),
  durationMinutes: z.number().int().positive(),
  note: z.string().trim().max(500).optional(),
  paymentMethod: z.literal("ACCOUNT_BALANCE").optional(),
});

export async function GET(request: Request) {
  try {
    const query = new URL(request.url).searchParams;
    const page = Math.max(0, Number(query.get("page")) || 0);
    const size = Math.min(30, Math.max(1, Number(query.get("size")) || 10));
    return NextResponse.json(await getMyBookings(page, size, {
      bookingDate: query.get("bookingDate") ?? undefined,
      status: query.get("status") ?? undefined,
    }));
  } catch (error) {
    return routeError(error);
  }
}

export async function POST(request: Request) {
  try {
    assertSameOrigin(request);
    const input = bookingSchema.parse(await request.json());
    return NextResponse.json(
      await createBooking(input),
    );
  } catch (error) {
    return routeError(error);
  }
}
