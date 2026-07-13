import "server-only";

import { API_BASE_URL, ApiError } from "./gateway";
import { getAccessToken, refreshSession } from "./session";

type ChatProxyResponse = {
  status: number;
  body: string;
  contentType: string;
};

function withForwardedHeaders(
  accessToken: string,
  request: Request,
): Headers {
  const headers = new Headers();
  headers.set("Authorization", `Bearer ${accessToken}`);
  headers.set("Content-Type", "application/json");
  headers.set("Accept", "application/json");

  const acceptLanguage = request.headers.get("Accept-Language");
  if (acceptLanguage) headers.set("Accept-Language", acceptLanguage);

  const timezone = request.headers.get("X-Timezone");
  if (timezone) headers.set("X-Timezone", timezone);

  return headers;
}

async function postChatWithToken(
  accessToken: string,
  request: Request,
  body: string,
): Promise<ChatProxyResponse> {
  let response: Response;
  try {
    response = await fetch(new URL("/api/chat", API_BASE_URL), {
      method: "POST",
      headers: withForwardedHeaders(accessToken, request),
      body,
      cache: "no-store",
      signal: AbortSignal.timeout(25_000),
    });
  } catch (error) {
    throw new ApiError(
      error instanceof Error && error.name === "TimeoutError"
        ? "The AI assistant took too long to respond"
        : "The AI assistant is currently unavailable",
      503,
      "CHAT_UNAVAILABLE",
    );
  }

  return {
    status: response.status,
    body: await response.text(),
    contentType: response.headers.get("content-type") ?? "application/json",
  };
}

export async function postChat(
  request: Request,
  body: string,
): Promise<ChatProxyResponse> {
  const accessToken = await getAccessToken();
  if (!accessToken) {
    throw new ApiError("Please sign in to use the AI assistant", 401, "UNAUTHENTICATED");
  }

  const first = await postChatWithToken(accessToken, request, body);
  if (first.status !== 401) return first;

  const refreshedAccessToken = await refreshSession();
  if (!refreshedAccessToken) return first;

  return postChatWithToken(refreshedAccessToken, request, body);
}
