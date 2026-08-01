import { NextResponse } from "next/server";
import { verifyOtpSchema } from "@/lib/api/auth-schemas";
import { assertSameOrigin, errorJson, routeError } from "@/lib/server/route-response";
import {
  setAccessCookie,
  setRefreshCookie,
} from "@/lib/server/session";

function getRefreshTokenFromSetCookie(setCookieHeader: string | null) {
  return setCookieHeader?.match(/(?:^|,\s*)refreshToken=([^;]+)/)?.[1];
}

export async function POST(request: Request) {
  try {
    assertSameOrigin(request);
    const input = verifyOtpSchema.parse(await request.json());

    const response = await fetch(
      new URL("/api/v1/auth/otp/verify", process.env.API_BASE_URL ?? "http://localhost:8080"),
      {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(input),
        cache: "no-store",
      },
    );

    if (!response.ok) {
      const error = await response.json().catch(() => null);
      return errorJson(response.status, error?.statusCode ?? error?.code ?? "INVALID_CREDENTIALS", "Verification failed.");
    }

    const payload = await response.json();
    const accessToken: unknown = payload?.data ?? payload?.accessToken;

    if (typeof accessToken !== "string" || !accessToken) {
      return errorJson(502, "SERVICE_UNAVAILABLE", "Invalid verify response.");
    }

    const nextResponse = NextResponse.json({ success: true });
    setAccessCookie(nextResponse.cookies, accessToken);

    const refreshToken = getRefreshTokenFromSetCookie(
      response.headers.get("set-cookie"),
    );
    if (!refreshToken) {
      return errorJson(502, "SERVICE_UNAVAILABLE", "Invalid verify response.");
    }
    setRefreshCookie(nextResponse.cookies, refreshToken);

    return nextResponse;
  } catch (error) {
    return routeError(error);
  }
}
