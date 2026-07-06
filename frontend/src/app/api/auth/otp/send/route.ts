import { NextResponse } from "next/server";
import { sendOtpSchema } from "@/lib/api/auth-schemas";
import { gatewayRequest } from "@/lib/server/gateway";
import { assertSameOrigin, routeError } from "@/lib/server/route-response";

export async function POST(request: Request) {
  try {
    assertSameOrigin(request);
    const input = sendOtpSchema.parse(await request.json());
    await gatewayRequest<null>("/api/v1/auth/otp/send", {
      method: "POST",
      body: JSON.stringify(input),
      cache: "no-store",
    });
    return NextResponse.json({ success: true });
  } catch (error) {
    return routeError(error);
  }
}
