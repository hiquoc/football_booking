import { cookies } from "next/headers";
import { NextResponse } from "next/server";
import { gatewayRequest } from "@/lib/server/gateway";
import { assertSameOrigin, routeError } from "@/lib/server/route-response";
import { clearSession, REFRESH_COOKIE } from "@/lib/server/session";

export async function POST(request: Request) {
  try {
    assertSameOrigin(request);
  } catch (error) {
    return routeError(error);
  }
  const refreshToken = (await cookies()).get(REFRESH_COOKIE)?.value;
  try {
    if (refreshToken) {
      await gatewayRequest<null>("/api/v1/auth/logout", {
        method: "POST",
        headers: { Cookie: `refreshToken=${refreshToken}` },
        cache: "no-store",
      });
    }
  } catch {
    // Local logout must still succeed when the backend is unavailable.
  } finally {
    await clearSession();
  }
  return NextResponse.json({ success: true });
}
