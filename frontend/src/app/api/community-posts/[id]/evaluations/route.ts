import { NextResponse } from "next/server";
import { z } from "zod";
import { submitMatchEvaluation } from "@/lib/server/community";

const schema = z.object({
  evaluatedUserId: z.string().uuid(),
  arrivedOnTime: z.boolean(),
  cancelledUnexpectedly: z.boolean(),
  fairPlay: z.boolean(),
  wouldPlayAgain: z.boolean(),
  comment: z.string().trim().max(1000).optional(),
});

export async function POST(request: Request, { params }: { params: Promise<{ id: string }> }) {
  const id = (await params).id;
  return NextResponse.json(await submitMatchEvaluation(id, schema.parse(await request.json())), { status: 201 });
}
