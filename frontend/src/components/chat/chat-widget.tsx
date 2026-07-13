"use client";

import { FormEvent, KeyboardEvent, useEffect, useRef, useState } from "react";
import { Bot, Loader2, MessageCircle, Send, X } from "lucide-react";
import { useChat } from "./chat-provider";

export function ChatWidget() {
  const { messages, isSending, sendMessage } = useChat();
  const [isOpen, setIsOpen] = useState(false);
  const [draft, setDraft] = useState("");
  const historyRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLTextAreaElement>(null);

  useEffect(() => {
    historyRef.current?.scrollTo({
      top: historyRef.current.scrollHeight,
      behavior: "smooth",
    });
  }, [messages, isSending, isOpen]);

  useEffect(() => {
    if (isOpen) inputRef.current?.focus();
  }, [isOpen]);

  async function handleSubmit(event?: FormEvent) {
    event?.preventDefault();
    const message = draft.trim();
    if (!message || isSending) return;
    setDraft("");
    await sendMessage(message);
  }

  function handleKeyDown(event: KeyboardEvent<HTMLTextAreaElement>) {
    if (event.key !== "Enter" || event.shiftKey) return;
    event.preventDefault();
    void handleSubmit();
  }

  return (
    <div className="fixed bottom-5 right-5 z-[70] flex flex-col items-end gap-3 sm:bottom-6 sm:right-6">
      {isOpen ? (
        <section className="flex h-[min(34rem,calc(100dvh-7rem))] w-[calc(100vw-2.5rem)] max-w-[24rem] flex-col overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-2xl shadow-slate-900/20">
          <header className="flex items-center justify-between border-b border-slate-200 bg-slate-950 px-4 py-3 text-white">
            <div className="flex min-w-0 items-center gap-2.5">
              <span className="grid size-9 shrink-0 place-items-center rounded-full bg-sky-400 text-slate-950">
                <Bot className="size-5" aria-hidden="true" />
              </span>
              <div className="min-w-0">
                <h2 className="truncate text-sm font-black">AI assistant</h2>
                <p className="text-xs font-semibold text-slate-300">
                  Booking support
                </p>
              </div>
            </div>
            <button
              type="button"
              className="grid size-9 place-items-center rounded-full text-slate-200 hover:bg-white/10 hover:text-white"
              onClick={() => setIsOpen(false)}
              aria-label="Close chat"
              title="Close chat"
            >
              <X className="size-5" aria-hidden="true" />
            </button>
          </header>

          <div
            ref={historyRef}
            className="flex-1 space-y-3 overflow-y-auto bg-slate-50 px-4 py-4"
          >
            {messages.map((message) => (
              <div
                key={message.id}
                className={
                  message.role === "user"
                    ? "flex justify-end"
                    : "flex justify-start"
                }
              >
                <div
                  className={[
                    "max-w-[85%] whitespace-pre-wrap rounded-2xl px-3.5 py-2.5 text-sm leading-6 shadow-sm",
                    message.role === "user"
                      ? "rounded-br-md bg-sky-500 font-semibold text-white"
                      : message.role === "error"
                        ? "rounded-bl-md border border-rose-200 bg-rose-50 font-semibold text-rose-700"
                        : "rounded-bl-md border border-slate-200 bg-white text-slate-700",
                  ].join(" ")}
                >
                  {message.text}
                </div>
              </div>
            ))}
            {isSending ? (
              <div className="flex justify-start">
                <div className="inline-flex items-center gap-2 rounded-2xl rounded-bl-md border border-slate-200 bg-white px-3.5 py-2.5 text-sm font-semibold text-slate-500 shadow-sm">
                  <Loader2 className="size-4 animate-spin" aria-hidden="true" />
                  Thinking
                </div>
              </div>
            ) : null}
          </div>

          <form
            className="border-t border-slate-200 bg-white p-3"
            onSubmit={handleSubmit}
          >
            <div className="flex items-end gap-2 rounded-2xl border border-slate-200 bg-slate-50 p-2 focus-within:border-sky-400 focus-within:bg-white focus-within:ring-4 focus-within:ring-sky-100">
              <textarea
                ref={inputRef}
                value={draft}
                onChange={(event) => setDraft(event.target.value)}
                onKeyDown={handleKeyDown}
                rows={1}
                maxLength={4000}
                placeholder="Ask about your booking"
                className="max-h-28 min-h-10 flex-1 resize-none bg-transparent px-2 py-2 text-sm font-medium text-slate-800 outline-none placeholder:text-slate-400"
              />
              <button
                type="submit"
                disabled={!draft.trim() || isSending}
                className="grid size-10 shrink-0 place-items-center rounded-full bg-sky-500 text-white hover:bg-sky-600 disabled:opacity-50"
                aria-label="Send message"
                title="Send message"
              >
                {isSending ? (
                  <Loader2 className="size-4 animate-spin" aria-hidden="true" />
                ) : (
                  <Send className="size-4" aria-hidden="true" />
                )}
              </button>
            </div>
          </form>
        </section>
      ) : null}

      <button
        type="button"
        className="grid size-14 place-items-center rounded-full bg-slate-950 text-white shadow-2xl shadow-slate-900/25 hover:bg-slate-800"
        onClick={() => setIsOpen((current) => !current)}
        aria-label="Open AI chat"
        title="Open AI chat"
      >
        {isOpen ? (
          <X className="size-6" aria-hidden="true" />
        ) : (
          <MessageCircle className="size-6" aria-hidden="true" />
        )}
      </button>
    </div>
  );
}
