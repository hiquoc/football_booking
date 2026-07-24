const dayNames: Record<string, string> = {
  MONDAY: "Thứ Hai",
  TUESDAY: "Thứ Ba",
  WEDNESDAY: "Thứ Tư",
  THURSDAY: "Thứ Năm",
  FRIDAY: "Thứ Sáu",
  SATURDAY: "Thứ Bảy",
  SUNDAY: "Chủ Nhật",
};

const labels: Record<string, string> = {
  FOOTBALL: "Bóng đá",
  FOOTBALL_5V5: "Sân bóng 5 người",
  FOOTBALL_7V7: "Sân bóng 7 người",
  FOOTBALL_11V11: "Sân bóng 11 người",
  BADMINTON_SINGLES: "Sân cầu lông đơn",
  BADMINTON_DOUBLES: "Sân cầu lông đôi",
  TENNIS_SINGLES: "Sân quần vợt đơn",
  TENNIS_DOUBLES: "Sân quần vợt đôi",
  BADMINTON: "Cầu lông",
  TENNIS: "Quần vợt",
  PICKLEBALL: "Pickleball",
  BASKETBALL: "Bóng rổ",
  VOLLEYBALL: "Bóng chuyền",
  INDOOR: "Trong nhà",
  OUTDOOR: "Ngoài trời",
  COVERED: "Có mái che",
  NATURAL_GRASS: "Cỏ tự nhiên",
  ARTIFICIAL_GRASS: "Cỏ nhân tạo",
  HARD_COURT: "Mặt sân cứng",
  CLAY: "Đất nện",
  WOOD: "Sàn gỗ",
  SYNTHETIC: "Mặt sân tổng hợp",
};

export function formatDay(day: string) {
  return dayNames[day] ?? formatEnum(day);
}

export function formatEnum(value: string | null | undefined) {
  if (!value) return "Chưa cập nhật";
  return (
    labels[value] ??
    value
      .toLowerCase()
      .replaceAll("_", " ")
      .replace(/(^|\s)\S/g, (c) => c.toUpperCase())
  );
}

export function formatTime(value: string | null) {
  return value?.slice(0, 5) ?? "--:--";
}

export function formatCurrency(value: number) {
  const amount = Math.round(Number.isFinite(value) ? value : 0);
  const sign = amount < 0 ? "-" : "";
  const digits = Math.abs(amount).toString();
  const formatted = digits.replace(/\B(?=(\d{3})+(?!\d))/g, ".");
  return `${sign}${formatted} ₫`;
}

export function formatDate(value: string) {
  return new Intl.DateTimeFormat("vi-VN", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
  }).format(new Date(value));
}

export function formatFieldAddress(
  field: Pick<Field, "address"> &
    Partial<Pick<Field, "ward" | "province" | "legacyWard" | "legacyDistrict" | "legacyProvince">>,
) {
  return Array.from(
    new Set(
      [
        field.address,
        field.legacyWard ?? field.ward,
        field.legacyDistrict,
        field.legacyProvince ?? field.province,
      ]
        .map((part) => part?.trim())
        .filter(Boolean),
    ),
  ).join(", ");
}
import type { Field } from "@/lib/api/types";
