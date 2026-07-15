import { NextResponse } from "next/server";
import { z } from "zod";
import { updateRecurringBooking } from "@/lib/server/recurring-bookings";
import { assertSameOrigin, routeError } from "@/lib/server/route-response";
import { uuidSchema } from "@/lib/api/common-schemas";

const recurringBookingSchema = z.object({
  subFieldId: uuidSchema,
  dayOfWeek: z.enum(["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"]),
  startTime: z.string().regex(/^\d{2}:\d{2}(:\d{2})?$/),
  endTime: z.string().regex(/^\d{2}:\d{2}(:\d{2})?$/),
  startDate: z.string().date(),
  endDate: z.string().date(),
});

export async function PUT(request: Request, { params }: { params: Promise<{ id: string }> }) {
  try {
    assertSameOrigin(request);
    const { id } = await params;
    const input = recurringBookingSchema.parse(await request.json());
    const withSeconds = (value: string) => value.length === 5 ? `${value}:00` : value;
    return NextResponse.json(await updateRecurringBooking(id, {
      ...input,
      startTime: withSeconds(input.startTime),
      endTime: withSeconds(input.endTime),
    }));
  } catch (error) {
    return routeError(error);
  }
}
