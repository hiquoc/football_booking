"use client";

import { LogOut } from "lucide-react";
import { useState } from "react";
import { useLogout } from "@/lib/hooks/use-auth";

export function LogoutButton() {
  const mutation = useLogout();
  const [leaving, setLeaving] = useState(false);

  async function logout() {
    setLeaving(true);
    try {
      await mutation.mutateAsync();
      window.location.replace("/");
    } catch {
      setLeaving(false);
    }
  }

  return (
    <>
      <button
        type="button"
        onClick={logout}
        disabled={mutation.isPending || leaving}
        aria-label={leaving ? "Đang đăng xuất" : "Đăng xuất"}
        title="Đăng xuất"
        className="grid size-10 place-items-center rounded-full border border-rose-200 text-rose-500 transition-colors duration-200 hover:bg-rose-500 hover:text-white disabled:opacity-50"
        >
        <LogOut className="size-5" aria-hidden="true" />
      </button>
      {leaving ? (
        <div
          className="fixed inset-0 z-[100] cursor-wait bg-white"
          aria-label="Đang đăng xuất"
        />
      ) : null}
    </>
  );
}
