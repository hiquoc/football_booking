import { NextResponse } from "next/server";
import type { FieldStatus } from "@/lib/api/types";
import { updateFieldStatus } from "@/lib/server/fields";
import { errorJson, routeError } from "@/lib/server/route-response";

const statuses = new Set<FieldStatus>(["PENDING", "APPROVED", "REJECTED"]);

export async function PATCH(
  request: Request,
  { params }: { params: Promise<{ id: string }> },
) {
  try {
    const body = (await request.json()) as { status?: FieldStatus };
    if (!body.status || !statuses.has(body.status)) {
      return errorJson(400, "VALIDATION_ERROR", "Validation failed.");
    }
    return NextResponse.json(await updateFieldStatus((await params).id, body.status));
  } catch (error) {
    return routeError(error);
  }
}
