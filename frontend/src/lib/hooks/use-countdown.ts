"use client";

import { useEffect, useState } from "react";

export function useCountdown(expiresAt: string | null | undefined) {
  const [now, setNow] = useState<number | null>(null);

  useEffect(() => {
    if (!expiresAt) return;
    const update = () => setNow(Date.now());
    const frame = window.requestAnimationFrame(update);
    const timer = window.setInterval(update, 1000);
    return () => {
      window.cancelAnimationFrame(frame);
      window.clearInterval(timer);
    };
  }, [expiresAt]);

  if (!expiresAt || now === null) return null;
  return Math.max(0, Math.ceil((new Date(expiresAt).getTime() - now) / 1000));
}
