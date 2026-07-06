import "server-only";

import { cookies } from "next/headers";
import type { User } from "@/lib/api/types";
import { ApiError, gatewayRequest } from "./gateway";

export const ACCESS_COOKIE = "fb_access_token";
export const REFRESH_COOKIE = "fb_refresh_token";

type CookieWriter = {
  set: (name: string, value: string, options?: Record<string, unknown>) => void;
};

function sharedCookieOptions() {
  return {
    httpOnly: true,
    secure: process.env.NODE_ENV === "production",
    sameSite: "lax" as const,
    path: "/",
  };
}

export function accessCookieOptions() {
  return {
    ...sharedCookieOptions(),
    maxAge: 60 * 60,
  };
}

export function refreshCookieOptions() {
  return {
    ...sharedCookieOptions(),
    maxAge: 7 * 24 * 60 * 60,
  };
}

export function expiredCookieOptions() {
  return {
    ...sharedCookieOptions(),
    maxAge: 0,
    expires: new Date(0),
  };
}

export function setAccessCookie(cookieStore: CookieWriter, accessToken: string) {
  cookieStore.set(ACCESS_COOKIE, accessToken, accessCookieOptions());
}

export function setRefreshCookie(cookieStore: CookieWriter, refreshToken: string) {
  cookieStore.set(REFRESH_COOKIE, refreshToken, refreshCookieOptions());
}

export async function setSession(accessToken: string, refreshToken: string) {
  const cookieStore = await cookies();
  setAccessCookie(cookieStore, accessToken);
  setRefreshCookie(cookieStore, refreshToken);
}

export async function clearSession() {
  const cookieStore = await cookies();
  cookieStore.set(ACCESS_COOKIE, "", expiredCookieOptions());
  cookieStore.set(REFRESH_COOKIE, "", expiredCookieOptions());
}

export async function getAccessToken() {
  return (await cookies()).get(ACCESS_COOKIE)?.value;
}

export async function getRefreshToken() {
  return (await cookies()).get(REFRESH_COOKIE)?.value;
}

async function requestCurrentUser(accessToken: string) {
  return gatewayRequest<User>("/api/v1/users/me", {
    headers: { Authorization: `Bearer ${accessToken}` },
    cache: "no-store",
  });
}

type RefreshedSession = {
  accessToken: string;
  refreshToken: string;
};

const refreshRequests = new Map<string, Promise<RefreshedSession | null>>();

function refreshTokenFromHeader(setCookieHeader: string | null) {
  return setCookieHeader?.match(/(?:^|,\s*)refreshToken=([^;]+)/)?.[1];
}

async function requestRefreshedSession(refreshToken: string) {
  const response = await fetch(
    new URL(
      "/api/v1/auth/refresh",
      process.env.API_BASE_URL ?? "http://localhost:8080",
    ),
    {
      method: "POST",
      headers: { Cookie: `refreshToken=${refreshToken}` },
      cache: "no-store",
    },
  );

  if (!response.ok) return null;

  const payload = await response.json().catch(() => null);
  const accessToken = payload?.data ?? payload?.accessToken;
  const rotatedRefreshToken = refreshTokenFromHeader(
    response.headers.get("set-cookie"),
  );
  if (typeof accessToken !== "string" || !rotatedRefreshToken) return null;

  return { accessToken, refreshToken: rotatedRefreshToken };
}

export async function refreshSession() {
  const refreshToken = await getRefreshToken();
  if (!refreshToken) return null;

  let refreshRequest = refreshRequests.get(refreshToken);
  if (!refreshRequest) {
    refreshRequest = requestRefreshedSession(refreshToken).finally(() => {
      refreshRequests.delete(refreshToken);
    });
    refreshRequests.set(refreshToken, refreshRequest);
  }

  const refreshedSession = await refreshRequest;
  if (!refreshedSession) return null;

  await setSession(
    refreshedSession.accessToken,
    refreshedSession.refreshToken,
  );
  return refreshedSession.accessToken;
}

export async function getCurrentUser(): Promise<User | null> {
  if (
    process.env.NODE_ENV !== "production" &&
    process.env.DEV_AUTH_BYPASS === "true"
  ) {
    return {
      id: "00000000-0000-0000-0000-000000000001",
      phoneNumber: "0000000000",
      email: "dev@example.com",
      fullName: "Development Owner",
      avatarUrl: null,
      userType: "OWNER",
      status: "ACTIVE",
    };
  }
  const accessToken = await getAccessToken();
  if (!accessToken) return null;

  try {
    return await requestCurrentUser(accessToken);
  } catch (error) {
    if (!(error instanceof ApiError) || error.status !== 401) {
      return null;
    }

    try {
      const refreshedAccessToken = await refreshSession();
      if (!refreshedAccessToken) return null;
      return await requestCurrentUser(refreshedAccessToken);
    } catch {
      return null;
    }
  }
}
