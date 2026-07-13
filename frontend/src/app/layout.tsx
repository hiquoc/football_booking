import type { Metadata } from "next";
import { ChatProvider } from "@/components/chat/chat-provider";
import { QueryProvider } from "@/components/providers/query-provider";
import { ToastProvider } from "@/components/providers/toast-provider";
import "leaflet/dist/leaflet.css";
import "./globals.css";

export const metadata: Metadata = {
  title: { default: "PitchUp — Đặt sân thể thao", template: "%s · PitchUp" },
  description: "Tìm và đặt sân thể thao chất lượng tại Thành phố Hồ Chí Minh.",
};

export default function RootLayout({
  children,
}: Readonly<{ children: React.ReactNode }>) {
  return (
    <html lang="vi">
      <body>
        <QueryProvider>
          <ToastProvider>
            <ChatProvider>{children}</ChatProvider>
          </ToastProvider>
        </QueryProvider>
      </body>
    </html>
  );
}
