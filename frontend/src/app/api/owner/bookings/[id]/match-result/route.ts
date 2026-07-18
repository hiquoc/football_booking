import { NextResponse } from "next/server";
import { z } from "zod";
import { upsertMatchResult } from "@/lib/server/bookings";
import { assertSameOrigin, routeError } from "@/lib/server/route-response";

const schema = z.object({
  winningTeam: z.enum(["TEAM_A", "TEAM_B", "DRAW"]),
  teamAPercentage: z.number().int().min(0).max(100),
  teamBPercentage: z.number().int().min(0).max(100),
});

export async function PUT(
  request: Request,
  { params }: { params: Promise<{ id: string }> },
) {
  try {
    assertSameOrigin(request);
    const { id } = await params;
    const body = schema.parse(await request.json());
    return NextResponse.json(await upsertMatchResult(id, body));
  } catch (error) {
    return routeError(error);
  }
}
