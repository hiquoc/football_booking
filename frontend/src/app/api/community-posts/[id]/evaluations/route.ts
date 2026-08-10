import { NextResponse } from "next/server";
import { z } from "zod";
import { getMatchEvaluations, submitMatchEvaluation } from "@/lib/server/community";

const schema = z.object({
  evaluatedUserId: z.string(),
  arrivedOnTime: z.boolean(),
  fairPlay: z.boolean(),
  wouldPlayAgain: z.boolean(),
  skillLevel: z.string().trim().min(1).max(40),
  comment: z.string().trim().max(1000).optional(),
});

export async function GET(_request: Request, { params }: { params: Promise<{ id: string }> }) {
  const id = (await params).id;
  return NextResponse.json(await getMatchEvaluations(id));
}

export async function POST(request: Request, { params }: { params: Promise<{ id: string }> }) {
  const id = (await params).id;
  return NextResponse.json(await submitMatchEvaluation(id, schema.parse(await request.json())), { status: 201 });
}
