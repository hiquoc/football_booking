import { NextResponse } from "next/server";
import { z } from "zod";
import { uuidSchema } from "@/lib/api/common-schemas";
import { createPaymentCheckout } from "@/lib/server/payments";
import { assertSameOrigin, routeError } from "@/lib/server/route-response";
const schema = z.object({
  bookingId: uuidSchema, amount: z.number().positive(), currency: z.string().length(3), provider: z.literal("STRIPE").optional(),
});
export async function POST(request: Request) {
  try { assertSameOrigin(request); return NextResponse.json(await createPaymentCheckout(schema.parse(await request.json()))); }
  catch (error) { 
    console.error("Error in /api/payments/checkout:", error);
    return routeError(error); }
}
