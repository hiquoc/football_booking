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
    return <span className="text-xs font-bold text-slate-400">Da cam</span>;
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
            setError(err instanceof Error ? err.message : "Khong the cam nguoi dung");
          } finally {
            setPending(false);
          }
        }}
        className="inline-flex justify-center rounded-lg bg-rose-600 px-3 py-2 text-xs font-black text-white hover:bg-rose-700 disabled:cursor-not-allowed disabled:opacity-60"
      >
        {pending ? "Dang cam..." : "Cam"}
      </button>
      {error ? <span className="max-w-40 text-xs font-semibold text-rose-600">{error}</span> : null}
    </div>
  );
}
