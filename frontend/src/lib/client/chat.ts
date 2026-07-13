import { jsonBody, requestJson } from "./http";

export type ChatRequest = {
  message: string;
  conversationId?: string;
};

export async function sendChatMessage(input: ChatRequest) {
  const request = jsonBody(input);
  return requestJson<unknown>("/api/chat", {
    method: "POST",
    ...request,
    headers: {
      ...request.headers,
      "X-Timezone": Intl.DateTimeFormat().resolvedOptions().timeZone,
    },
  });
}
