import { NextResponse } from "next/server";
import { getPayment } from "@/lib/server/payments";
import { routeError } from "@/lib/server/route-response";
export async function GET(_request: Request, { params }: { params: Promise<{ bookingId: string }> }) {
  try { return NextResponse.json(await getPayment((await params).bookingId)); }
  catch (error) {
     return routeError(error); }
}
