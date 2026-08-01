import { NextResponse } from "next/server";
import { z } from "zod";
import { createReservation, getOwnerReservations } from "@/lib/server/bookings";
import { uuidSchema } from "@/lib/api/common-schemas";
import { assertSameOrigin, routeError } from "@/lib/server/route-response";

const reservationSchema = z.object({
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
    return NextResponse.json(
      await getOwnerReservations(
        Math.max(0, Number(query.get("page")) || 0),
        Math.min(30, Math.max(1, Number(query.get("size")) || 10)),
        {
          bookingDate: query.get("bookingDate")?.trim() || undefined,
          subFieldId: query.get("subFieldId")?.trim() || undefined,
          status: query.get("status")?.trim() || undefined,
        },
      ),
    );
  } catch (error) {
    return routeError(error);
  }
}

export async function POST(request: Request) {
  try {
    assertSameOrigin(request);
    const input = reservationSchema.parse(await request.json());
    return NextResponse.json(await createReservation(input));
  } catch (error) {
    return routeError(error);
  }
}
