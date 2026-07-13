import { NextResponse } from "next/server";
import { z } from "zod";
import { postChat } from "@/lib/server/chat";
import { assertSameOrigin, routeError } from "@/lib/server/route-response";

const chatSchema = z.object({
  message: z.string().trim().min(1, "Message is required").max(4000),
  conversationId: z.string().uuid().optional(),
});

export async function POST(request: Request) {
  try {
    assertSameOrigin(request);
    const input = chatSchema.parse(await request.json());
    const response = await postChat(request, JSON.stringify(input));

    return new Response(response.body, {
      status: response.status,
      headers: { "Content-Type": response.contentType },
    });
  } catch (error) {
    return routeError(error);
  }
}
