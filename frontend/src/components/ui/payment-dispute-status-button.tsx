import type { PaymentDisputeStatus } from "@/lib/api/types";

const paymentDisputeStatusLabels: Record<PaymentDisputeStatus, string> = {
  PENDING: "Chờ xử lý",
  APPROVED: "Đã chấp nhận",
  REJECTED: "Đã từ chối",
};

const paymentDisputeStatusClassName: Record<PaymentDisputeStatus, string> = {
  PENDING: "bg-amber-500 text-white",
  APPROVED: "bg-green-600 text-white",
  REJECTED: "bg-rose-500 text-white",
};

export function PaymentDisputeStatusButton({
  status,
  size = "md",
  className = "",
}: {
  status: PaymentDisputeStatus;
  size?: "sm" | "md" | "lg";
  className?: string;
}) {
  const sizeClassName =
    size === "lg"
      ? "h-12 rounded-xl px-5 text-base leading-none"
      : size === "sm"
        ? "h-8 rounded-lg px-3 text-xs leading-none"
        : "h-10 rounded-lg px-4 text-sm leading-none";

  return (
    <span
      className={`inline-flex items-center justify-center font-black shadow-sm ${sizeClassName} ${paymentDisputeStatusClassName[status]} ${className}`}
      aria-label={paymentDisputeStatusLabels[status]}
    >
      {paymentDisputeStatusLabels[status]}
    </span>
  );
}
