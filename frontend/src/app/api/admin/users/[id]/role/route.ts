import { NextResponse } from "next/server";
import type { User } from "@/lib/api/types";
import { updateUserRole } from "@/lib/server/users";
import { routeError } from "@/lib/server/route-response";

const roles = new Set<User["userType"]>(["CLIENT", "OWNER", "EMPLOYEE", "ADMIN"]);

export async function PUT(
  request: Request,
  { params }: { params: Promise<{ id: string }> },
) {
  try {
    const body = (await request.json()) as { userType?: User["userType"] };
    if (!body.userType || !roles.has(body.userType)) {
      return NextResponse.json({ message: "Vai trò không hợp lệ" }, { status: 400 });
    }
    return NextResponse.json(
      await updateUserRole((await params).id, body.userType),
    );
  } catch (error) {
    return routeError(error);
  }
}
