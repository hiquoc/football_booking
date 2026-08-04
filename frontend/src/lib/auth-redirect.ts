export const DEFAULT_AUTH_REDIRECT_PATH = "/";
export const AUTH_REDIRECT_PARAM = "redirect";

export function requestedPathFromUrl(url: URL): string {
  return `${url.pathname}${url.search}`;
}

export function isSafeInternalRedirect(
  value: string | null | undefined,
): value is string {
  if (!value || value.trim() !== value) return false;
  if (!value.startsWith("/") || value.startsWith("//")) return false;
  if (/[\u0000-\u001F\u007F]/.test(value)) return false;

  try {
    const parsed = new URL(value, "http://internal.local");
    return parsed.origin === "http://internal.local" && parsed.pathname.startsWith("/");
  } catch {
    return false;
  }
}

export function safeAuthRedirect(
  value: string | null | undefined,
  fallback = DEFAULT_AUTH_REDIRECT_PATH,
): string {
  return isSafeInternalRedirect(value) ? value : fallback;
}
