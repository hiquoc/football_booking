import {
  localizedErrorMessage,
  statusCodeFromHttpStatus,
  statusCodeFromPayload,
} from "../error-messages";

let refreshPromise: Promise<boolean> | null = null;

export class ClientRequestError extends Error {
  constructor(
    message: string,
    public readonly status: number,
    public readonly statusCode: string,
    public readonly developerMessage?: string,
  ) {
    super(message);
    this.name = "ClientRequestError";
  }
}

function buildHeaders(init?: RequestInit) {
  return {
    Accept: "application/json",
    ...init?.headers,
  };
}

function shouldRefreshFor401(path: string) {
  return path.startsWith("/api/") && !path.startsWith("/api/auth/");
}

async function refreshSession() {
  if (!refreshPromise) {
    refreshPromise = fetch("/api/auth/refresh", {
      method: "POST",
      credentials: "same-origin",
      headers: { Accept: "application/json" },
      cache: "no-store",
    })
      .then((response) => response.ok)
      .finally(() => {
        refreshPromise = null;
      });
  }

  return refreshPromise;
}

async function fetchJson<T>(path: string, init?: RequestInit) {
  const response = await fetch(path, {
    ...init,
    credentials: "same-origin",
    headers: buildHeaders(init),
    cache: init?.cache ?? "no-store",
  });

  const payload = (await response.json().catch(() => null)) as
    | T
    | { message?: string; statusCode?: string; code?: string }
    | null;

  return { response, payload };
}

function payloadDeveloperMessage(payload: unknown) {
  return payload && typeof payload === "object" && "message" in payload
    ? (payload.message as string | undefined)
    : undefined;
}

function throwRequestError(payload: unknown, status: number) {
  const payloadStatusCode = statusCodeFromPayload(payload);
  const statusCode =
    payloadStatusCode === "UNKNOWN_ERROR"
      ? statusCodeFromHttpStatus(status)
      : payloadStatusCode;
  throw new ClientRequestError(
    localizedErrorMessage(statusCode),
    status,
    statusCode,
    payloadDeveloperMessage(payload),
  );
}

export async function requestJson<T>(
  path: string,
  init?: RequestInit,
): Promise<T> {
  let { response, payload } = await fetchJson<T>(path, init);

  if (response.status === 401 && shouldRefreshFor401(path)) {
    const refreshed = await refreshSession();
    if (refreshed) {
      const retry = await fetchJson<T>(path, init);
      response = retry.response;
      payload = retry.payload;
    }
  }

  if (!response.ok) {
    throwRequestError(payload, response.status);
  }

  return payload as T;
}

export function jsonBody(body: unknown): RequestInit {
  return {
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body),
  };
}
