import "server-only";

import { ApiError, gatewayRequest } from "./gateway";
import { getAccessToken, refreshSession } from "./session";

type GatewayInit = RequestInit & {
  next?: { revalidate?: number; tags?: string[] };
};

function withBearerToken(init: GatewayInit, accessToken: string): GatewayInit {
  const headers = new Headers(init.headers);
  headers.set("Authorization", `Bearer ${accessToken}`);
  return {
    method: init.method,
    body: init.body,
    signal: init.signal,
    headers,
    next: init.next,
    cache: "no-store" as const,
  };
}

export async function authenticatedGatewayRequest<T>(
  path: string,
  init: GatewayInit = {},
) {
  const accessToken = await getAccessToken();
  if (!accessToken) {
    throw new ApiError("Session has expired", 401, "UNAUTHENTICATED");
  }

  try {
    return await gatewayRequest<T>(path, withBearerToken(init, accessToken));
  } catch (error) {
    if (!(error instanceof ApiError) || error.status !== 401) throw error;

    const refreshedAccessToken = await refreshSession();
    if (!refreshedAccessToken) {
      throw new ApiError("Session has expired", 401, "UNAUTHENTICATED");
    }

    return gatewayRequest<T>(
      path,
      withBearerToken(init, refreshedAccessToken),
    );
  }
}

export async function sessionGatewayRequest<T>(
  path: string,
  init: GatewayInit = {},
) {
  const accessToken = await getAccessToken();
  if (!accessToken) {
    return gatewayRequest<T>(path, init);
  }

  try {
    return await gatewayRequest<T>(path, withBearerToken(init, accessToken));
  } catch (error) {
    if (!(error instanceof ApiError) || error.status !== 401) throw error;

    const refreshedAccessToken = await refreshSession();
    if (!refreshedAccessToken) throw error;

    return gatewayRequest<T>(
      path,
      withBearerToken(init, refreshedAccessToken),
    );
  }
}
