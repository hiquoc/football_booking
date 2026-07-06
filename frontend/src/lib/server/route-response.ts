import { NextResponse } from "next/server";
import { ZodError } from "zod";
import { ApiError } from "./gateway";

export function assertSameOrigin(request: Request) {
  const origin = request.headers.get("Origin");
  if (origin && new URL(origin).host !== new URL(request.url).host) {
    throw new ApiError("Cross-origin request rejected", 403, "INVALID_ORIGIN");
  }
}

export function routeError(error: unknown) {
  if (error instanceof ZodError) {
    return NextResponse.json(
      { message: error.issues[0]?.message ?? "Invalid request" },
      { status: 400 },
    );
  }
  if (error instanceof ApiError) {
    return NextResponse.json(
      { message: error.message, code: error.code },
      { status: error.status },
    );
  }
  return NextResponse.json(
    { message: "An unexpected error occurred" },
    { status: 500 },
  );
}
