import { NextResponse } from "next/server";
import { getAdminPaymentDisputes } from "@/lib/server/moderation";
import { routeError } from "@/lib/server/route-response";
import type { PaymentDisputeStatus } from "@/lib/api/types";

export async function GET(request: Request) {
  try {
    const query = new URL(request.url).searchParams;
    return NextResponse.json(await getAdminPaymentDisputes(
      Number(query.get("page")) || 0,
      Number(query.get("size")) || 20,
      (query.get("status") || undefined) as PaymentDisputeStatus | undefined,
    ));
  } catch (error) {
    return routeError(error);
  }
}
