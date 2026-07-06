import { NextResponse } from "next/server";
import { requestFieldImageUploadSlots } from "@/lib/server/fields";
import { assertSameOrigin, routeError } from "@/lib/server/route-response";

export async function POST(request: Request, { params }: { params: Promise<{ id: string }> }) {
  try {
    assertSameOrigin(request);
    const body = (await request.json()) as { requestId?: string; count?: number };
    if (!body.requestId || !Number.isInteger(body.count) || body.count! < 1 || body.count! > 10) {
      return NextResponse.json({ message: "Yêu cầu tải ảnh không hợp lệ" }, { status: 400 });
    }
    return NextResponse.json(await requestFieldImageUploadSlots((await params).id, body.requestId, body.count!));
  } catch (error) { return routeError(error); }
}
