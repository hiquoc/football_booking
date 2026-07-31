import { NextResponse } from "next/server";
import { z } from "zod";
import { assignFieldEmployee, getFieldEmployees } from "@/lib/server/fields";
import { assertSameOrigin, routeError } from "@/lib/server/route-response";

const javaUuidSchema = z.string().regex(
  /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i,
  "Invalid UUID",
);
const schema = z.object({ employeeId: javaUuidSchema });

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
