import "server-only";

import type { ApiResponse, ErrorResponse } from "@/lib/api/types";

const API_BASE_URL = process.env.API_BASE_URL ?? "http://localhost:8080";

export class ApiError extends Error {
  constructor(
    message: string,
    public readonly status: number,
    public readonly code?: string | null,
  ) {
    super(message);
    this.name = "ApiError";
  }
}

export async function gatewayRequest<T>(
  path: string,
  init: RequestInit & { next?: { revalidate?: number; tags?: string[] } } = {},
): Promise<T> {
  const headers = new Headers(init.headers);
  if (
    init.body &&
    !(init.body instanceof FormData) &&
    !headers.has("Content-Type")
  ) {
    headers.set("Content-Type", "application/json");
  }
  headers.set("Accept", "application/json");

  let response: Response;
  try {
    response = await fetch(new URL(path, API_BASE_URL), {
      ...init,
      headers,
      signal: init.signal ?? AbortSignal.timeout(8_000),
    });
  } catch (error) {
    throw new ApiError(
      error instanceof Error && error.name === "TimeoutError"
        ? "The service took too long to respond"
        : "The service is currently unavailable",
      503,
      "SERVICE_UNAVAILABLE",
    );
  }

  const payload = (await response.json().catch(() => null)) as
    | ApiResponse<T>
    | ErrorResponse
    | null;

  if (!response.ok) {
    const error = payload as ErrorResponse | null;
    throw new ApiError(
      error?.message ?? "The request could not be completed",
      response.status,
      error?.code,
    );
  }

  const envelope = payload as ApiResponse<T> | null;
  if (!envelope?.success) {
    throw new ApiError(
      envelope?.message ?? "Invalid API response",
      502,
      "INVALID_RESPONSE",
    );
  }

  return envelope.data;
}
