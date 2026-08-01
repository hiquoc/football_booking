import { NextResponse } from "next/server";
import type { User } from "@/lib/api/types";
import { updateUserRole } from "@/lib/server/users";
import { assertSameOrigin, errorJson, routeError } from "@/lib/server/route-response";

const roles = new Set<User["userType"]>(["CLIENT", "OWNER", "EMPLOYEE"]);

export async function PUT(
  request: Request,
  { params }: { params: Promise<{ id: string }> },
) {
  try {
    assertSameOrigin(request);
    const body = (await request.json()) as { userType?: User["userType"] };
    if (!body.userType || !roles.has(body.userType)) {
      return errorJson(400, "VALIDATION_ERROR", "Validation failed.");
    }
    return NextResponse.json(
      await updateUserRole((await params).id, body.userType),
    );
  } catch (error) {
    return routeError(error);
  }
}
