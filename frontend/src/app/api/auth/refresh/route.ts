import { NextResponse } from "next/server";
import { assertSameOrigin, routeError } from "@/lib/server/route-response";
import {
  clearSession,
  REFRESH_COOKIE,
  setAccessCookie,
  setRefreshCookie,
} from "@/lib/server/session";

function getRefreshTokenFromSetCookie(setCookieHeader: string | null) {
  return setCookieHeader?.match(/(?:^|,\s*)refreshToken=([^;]+)/)?.[1];
}

export async function POST(request: Request) {
  try {
    assertSameOrigin(request);
    const cookieHeader = request.headers.get("cookie") ?? "";
    const refreshToken = cookieHeader.match(
      new RegExp(`(?:^|;\\s*)${REFRESH_COOKIE}=([^;]+)`),
    )?.[1];
    if (!refreshToken) {
      return NextResponse.json(
        { message: "Session has expired" },
        { status: 401 },
      );
    }

    const response = await fetch(
      new URL("/api/v1/auth/refresh", process.env.API_BASE_URL ?? "http://localhost:8080"),
      {
        method: "POST",
        headers: { Cookie: `refreshToken=${refreshToken}` },
        cache: "no-store",
      },
    );

    if (!response.ok) {
      const error = await response.json().catch(() => null);
      await clearSession();
      return NextResponse.json(
        { message: error?.message ?? "Session has expired" },
        { status: 401 },
      );
    }

    const payload = await response.json();
    const accessToken: unknown = payload?.data ?? payload?.accessToken;

    if (typeof accessToken !== "string" || !accessToken) {
      await clearSession();
      return NextResponse.json(
        { message: "Invalid refresh response" },
        { status: 502 },
      );
    }

    const nextResponse = NextResponse.json({ success: true });
    setAccessCookie(nextResponse.cookies, accessToken);

    const rotatedRefreshToken = getRefreshTokenFromSetCookie(
      response.headers.get("set-cookie"),
    );
    if (!rotatedRefreshToken) {
      await clearSession();
      return NextResponse.json(
        { message: "Invalid refresh response: refresh cookie is missing" },
        { status: 502 },
      );
    }
    setRefreshCookie(nextResponse.cookies, rotatedRefreshToken);

    return nextResponse;
  } catch (error) {
    await clearSession();
    return routeError(error);
  }
}
