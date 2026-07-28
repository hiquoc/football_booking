import { NextResponse } from "next/server";
import { z } from "zod";
import { assignFieldEmployee, getFieldEmployees } from "@/lib/server/fields";
import { assertSameOrigin, routeError } from "@/lib/server/route-response";

const schema = z.object({ employeeId: z.string().uuid() });

export async function GET(
  _request: Request,
  { params }: { params: Promise<{ id: string }> },
) {
  try {
    return NextResponse.json(await getFieldEmployees((await params).id));
  } catch (error) {
    return routeError(error);
  }
}

export async function POST(
  request: Request,
  { params }: { params: Promise<{ id: string }> },
) {
  try {
    assertSameOrigin(request);
    const body = schema.parse(await request.json());
    return NextResponse.json(await assignFieldEmployee((await params).id, body.employeeId));
  } catch (error) {
    return routeError(error);
  }
}
