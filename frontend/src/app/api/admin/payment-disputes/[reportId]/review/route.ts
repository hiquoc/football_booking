import { NextResponse } from "next/server";
import { z } from "zod";
import { reviewPaymentDispute } from "@/lib/server/moderation";
import { assertSameOrigin, routeError } from "@/lib/server/route-response";

const schema = z.object({ approved: z.boolean(), adminNote: z.string().trim().min(1) });

export async function PATCH(request: Request, { params }: { params: Promise<{ reportId: string }> }) {
  try {
    assertSameOrigin(request);
    const { reportId } = await params;
    const input = schema.parse(await request.json());
    return NextResponse.json(await reviewPaymentDispute(reportId, input.approved, input.adminNote));
  } catch (error) {
    return routeError(error);
  }
}
