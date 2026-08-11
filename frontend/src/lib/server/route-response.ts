import { NextResponse } from "next/server";
import { ZodError } from "zod";
import { ApiError } from "./gateway";

export function errorJson(
  status: number,
  statusCode: string,
  message: string,
) {
  return NextResponse.json(
    {
      success: false,
      status,
      statusCode,
      code: statusCode,
      message,
      data: null,
      timestamp: new Date().toISOString(),
    },
    { status },
  );
}

export function assertSameOrigin(request: Request) {
  const origin = request.headers.get("Origin");
  const requestHost =
    request.headers.get("X-Forwarded-Host") ??
    request.headers.get("Host") ??
    new URL(request.url).host;

  if (origin && new URL(origin).host !== requestHost) {
    throw new ApiError("Cross-origin request rejected", 403, "INVALID_ORIGIN");
  }
}

export function routeError(error: unknown) {
  if (error instanceof ZodError) {
    return errorJson(400, "VALIDATION_ERROR", "Validation failed.");
  }
  if (error instanceof ApiError) {
    const statusCode = error.statusCode ?? "UNKNOWN_ERROR";
    return errorJson(error.status, statusCode, error.message);
  }
  return errorJson(500, "INTERNAL_ERROR", "Internal server error.");
}
