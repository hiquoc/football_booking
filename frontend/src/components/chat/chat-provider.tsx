"use client";

import {
  createContext,
  useCallback,
  useContext,
  useMemo,
  useState,
  type ReactNode,
} from "react";
import { sendChatMessage } from "@/lib/client/chat";

type ChatMessage = {
  id: string;
  role: "user" | "assistant" | "error";
  text: string;
};

type ChatContextValue = {
  conversationId: string;
  messages: ChatMessage[];
  isSending: boolean;
  sendMessage: (message: string) => Promise<void>;
};

const ChatContext = createContext<ChatContextValue | null>(null);

function createId() {
  return crypto.randomUUID();
}

function responseText(payload: unknown): string {
  if (typeof payload === "string") return payload;
  if (!payload || typeof payload !== "object") return "I received an empty response.";

  const object = payload as Record<string, unknown>;
  for (const key of ["reply", "response", "message", "text", "answer", "output"]) {
    if (typeof object[key] === "string") return object[key] as string;
  }

  if (object.data && typeof object.data === "object") {
    return responseText(object.data);
  }

  return JSON.stringify(payload, null, 2);
}

export function ChatProvider({ children }: { children: ReactNode }) {
  const [conversationId] = useState(createId);
  const [messages, setMessages] = useState<ChatMessage[]>([
    {
      id: createId(),
      role: "assistant",
      text: "Hi, I can help with bookings, fields, and account questions.",
    },
  ]);
  const [isSending, setIsSending] = useState(false);

  const sendMessage = useCallback(
    async (message: string) => {
      const trimmed = message.trim();
      if (!trimmed || isSending) return;

      setIsSending(true);
      setMessages((current) => [
        ...current,
        { id: createId(), role: "user", text: trimmed },
      ]);

      try {
        const response = await sendChatMessage({ message: trimmed, conversationId });
        setMessages((current) => [
          ...current,
          { id: createId(), role: "assistant", text: responseText(response) },
        ]);
      } catch (error) {
        setMessages((current) => [
          ...current,
          {
            id: createId(),
            role: "error",
            text:
              error instanceof Error
                ? error.message
                : "The AI assistant is currently unavailable.",
          },
        ]);
      } finally {
        setIsSending(false);
      }
    },
    [conversationId, isSending],
  );

  const value = useMemo(
    () => ({ conversationId, messages, isSending, sendMessage }),
    [conversationId, messages, isSending, sendMessage],
  );

  return <ChatContext.Provider value={value}>{children}</ChatContext.Provider>;
}

export function useChat() {
  const value = useContext(ChatContext);
  if (!value) throw new Error("useChat must be used within ChatProvider");
  return value;
}
