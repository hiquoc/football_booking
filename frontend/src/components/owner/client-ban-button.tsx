"use client";

import { useRouter } from "next/navigation";
import { useState } from "react";
import { submitBanClient } from "@/lib/client/moderation";

export function ClientBanButton({
  fieldId,
  userId,
  banned,
}: {
  fieldId: string;
  userId: string;
  banned: boolean;
}) {
  const router = useRouter();
  const [pending, setPending] = useState(false);
  const [error, setError] = useState<string | null>(null);

  if (banned) {
    return (
      <span className="inline-flex h-8 items-center rounded-lg bg-slate-500 px-3 text-xs font-black text-white">
        Đã cấm
      </span>
    );
  }

  return (
    <div className="flex flex-col gap-1">
      <button
        type="button"
        disabled={pending}
        onClick={async () => {
          setPending(true);
          setError(null);
          try {
            await submitBanClient(fieldId, userId);
            router.refresh();
          } catch (err) {
            setError(err instanceof Error ? err.message : "Không thể cấm người dùng");
          } finally {
            setPending(false);
          }
        }}
        className="inline-flex justify-center rounded-lg bg-rose-600 px-3 py-2 text-xs font-black text-white disabled:cursor-not-allowed disabled:opacity-60"
      >
        {pending ? "Đang cấm..." : "Cấm"}
      </button>
      {error ? <span className="max-w-40 text-xs font-semibold text-rose-600">{error}</span> : null}
    </div>
  );
}
