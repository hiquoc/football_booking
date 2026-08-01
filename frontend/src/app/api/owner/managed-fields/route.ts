import { NextResponse } from "next/server";
import { getAssignedFields, getOwnerFields } from "@/lib/server/fields";
import { requireUser } from "@/lib/server/guards";
import { routeError } from "@/lib/server/route-response";

export async function GET(request: Request) {
  try {
    const user = await requireUser();
    const query = new URL(request.url).searchParams;
    const page = Math.max(0, Number(query.get("page")) || 0);
    const size = Math.min(100, Math.max(1, Number(query.get("size")) || 10));
    return NextResponse.json(
      user.userType === "EMPLOYEE"
        ? await getAssignedFields(page, size)
        : await getOwnerFields(page, size),
      {
        headers: {
          "Cache-Control": "private, max-age=60",
        },
      },
    );
  } catch (error) {
    return routeError(error);
  }
}
