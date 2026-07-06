import { NextRequest, NextResponse } from "next/server";
import {
  ACCESS_COOKIE,
  REFRESH_COOKIE,
  accessCookieOptions,
  expiredCookieOptions,
  refreshCookieOptions,
} from "@/lib/server/session";

const protectedPrefixes = [
  "/bookings",
  "/profile",
  "/notifications",
  "/owner",
  "/admin",
];

function tokenIsExpired(token?: string) {
  if (!token) return true;
  try {
    const payload = JSON.parse(
      Buffer.from(token.split(".")[1], "base64url").toString("utf8"),
    ) as { exp?: number };
    return !payload.exp || payload.exp * 1000 <= Date.now() + 5_000;
  } catch {
    return true;
  }
}

async function refreshAccessToken(request: NextRequest, refreshToken: string) {
  const response = await fetch(
    new URL("/api/v1/auth/refresh", process.env.API_BASE_URL ?? "http://localhost:8080"),
    {
      method: "POST",
      headers: { Cookie: `refreshToken=${refreshToken}` },
      cache: "no-store",
    },
  );
  if (!response.ok) return null;

  const payload = await response.json().catch(() => null);
  const accessToken = payload?.data ?? payload?.accessToken;
  if (typeof accessToken !== "string") return null;

  const rotatedRefreshToken = response.headers
    .get("set-cookie")
    ?.match(/(?:^|,\s*)refreshToken=([^;]+)/)?.[1];
  const requestHeaders = new Headers(request.headers);
  const forwardedCookies = request.cookies
    .getAll()
    .filter(({ name }) => name !== ACCESS_COOKIE && name !== REFRESH_COOKIE)
    .map(({ name, value }) => `${name}=${value}`);
  forwardedCookies.push(`${ACCESS_COOKIE}=${accessToken}`);
  forwardedCookies.push(`${REFRESH_COOKIE}=${rotatedRefreshToken ?? refreshToken}`);
  requestHeaders.set("cookie", forwardedCookies.join("; "));

  const nextResponse = NextResponse.next({
    request: { headers: requestHeaders },
  });
  nextResponse.cookies.set(ACCESS_COOKIE, accessToken, accessCookieOptions());

  if (rotatedRefreshToken) {
    nextResponse.cookies.set(
      REFRESH_COOKIE,
      rotatedRefreshToken,
      refreshCookieOptions(),
    );
  }
  return nextResponse;
}

function clearSessionCookies(response: NextResponse) {
  response.cookies.set(ACCESS_COOKIE, "", expiredCookieOptions());
  response.cookies.set(REFRESH_COOKIE, "", expiredCookieOptions());
  return response;
}

export async function proxy(request: NextRequest) {
  if (
    process.env.NODE_ENV !== "production" &&
    process.env.DEV_AUTH_BYPASS === "true"
  ) {
    return NextResponse.next();
  }
  const isProtected = protectedPrefixes.some((prefix) =>
    request.nextUrl.pathname.startsWith(prefix),
  );
  const accessToken = request.cookies.get(ACCESS_COOKIE)?.value;
  const refreshToken = request.cookies.get(REFRESH_COOKIE)?.value;
  const accessTokenExpired = tokenIsExpired(accessToken);
  let hasValidSession = Boolean(accessToken && !accessTokenExpired);
  let hasRefreshToken = Boolean(refreshToken);
  let refreshFailed = false;

  if (accessTokenExpired && refreshToken) {
    const refreshed = await refreshAccessToken(request, refreshToken);
    if (refreshed) return refreshed;
    hasRefreshToken = false;
    refreshFailed = true;
  }

  if (!accessTokenExpired && accessToken) {
    hasValidSession = true;
  }

  if (isProtected && !hasValidSession && !hasRefreshToken) {
    const loginUrl = new URL("/auth/login", request.url);
    loginUrl.searchParams.set("next", request.nextUrl.pathname);
    return clearSessionCookies(NextResponse.redirect(loginUrl));
  }

  if (request.nextUrl.pathname === "/auth/login" && hasValidSession) {
    return NextResponse.redirect(new URL("/", request.url));
  }

  if (refreshFailed) {
    return clearSessionCookies(NextResponse.next());
  }

  return NextResponse.next();
}

export const config = {
  matcher: [
    "/((?!api|_next/static|_next/image|favicon.ico).*)",
  ],
};
