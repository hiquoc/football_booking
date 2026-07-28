import { NextResponse } from "next/server";
import { findEmployeeByPhone } from "@/lib/server/users";
import { routeError } from "@/lib/server/route-response";

export async function GET(request: Request) {
  try {
    const query = new URL(request.url).searchParams;
    return NextResponse.json(await findEmployeeByPhone(query.get("phoneNumber") ?? ""));
  } catch (error) {
    return routeError(error);
  }
}
