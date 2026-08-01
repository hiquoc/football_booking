import type { Field } from "@/lib/api/types";

export const fieldTypeLabels: Record<string, string> = {
  FOOTBALL: "Bóng đá",
  BASKETBALL: "Bóng rổ",
  BADMINTON: "Cầu lông",
  VOLLEYBALL: "Bóng chuyền",
  TENNIS: "Quần vợt",
  PICKLEBALL: "Pickleball",
  TABLE_TENNIS: "Bóng bàn",
  SWIMMING: "Bơi lội",
  YOGA: "Yoga",
  GYM: "Gym",
  DANCE: "Nhảy",
  MARTIAL_ARTS: "Võ thuật",
  OTHER: "Khác",
};

export const subFieldTypeLabels: Record<string, string> = {
  FOOTBALL_5V5: "Bóng đá 5 người",
  FOOTBALL_7V7: "Bóng đá 7 người",
  FOOTBALL_11V11: "Bóng đá 11 người",
  BASKETBALL_HALF_COURT: "Bóng rổ nửa sân",
  BASKETBALL_FULL_COURT: "Bóng rổ toàn sân",
  BADMINTON_SINGLES: "Cầu lông đơn",
  BADMINTON_DOUBLES: "Cầu lông đôi",
  TENNIS_SINGLES: "Quần vợt đơn",
  TENNIS_DOUBLES: "Quần vợt đôi",
  BADMINTON: "Cầu lông",
  VOLLEYBALL: "Bóng chuyền",
  TENNIS: "Quần vợt",
};

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
  ...fieldTypeLabels,
  ...subFieldTypeLabels,
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

export const fieldTypeOptions = [
  ["FOOTBALL", fieldTypeLabels.FOOTBALL],
  ["BASKETBALL", fieldTypeLabels.BASKETBALL],
  ["BADMINTON", fieldTypeLabels.BADMINTON],
  ["VOLLEYBALL", fieldTypeLabels.VOLLEYBALL],
  ["TENNIS", fieldTypeLabels.TENNIS],
] as const;

export const communityFieldTypeOptions = [
  ["FOOTBALL_5V5", subFieldTypeLabels.FOOTBALL_5V5],
  ["FOOTBALL_7V7", subFieldTypeLabels.FOOTBALL_7V7],
  ["FOOTBALL_11V11", subFieldTypeLabels.FOOTBALL_11V11],
  ["BASKETBALL_HALF_COURT", subFieldTypeLabels.BASKETBALL_HALF_COURT],
  ["BASKETBALL_FULL_COURT", subFieldTypeLabels.BASKETBALL_FULL_COURT],
  ["BADMINTON", subFieldTypeLabels.BADMINTON],
  ["VOLLEYBALL", subFieldTypeLabels.VOLLEYBALL],
  ["TENNIS", subFieldTypeLabels.TENNIS],
] as const;

export const subFieldTypeOptions = [
  ["FOOTBALL_5V5", subFieldTypeLabels.FOOTBALL_5V5],
  ["FOOTBALL_7V7", subFieldTypeLabels.FOOTBALL_7V7],
  ["FOOTBALL_11V11", subFieldTypeLabels.FOOTBALL_11V11],
  ["BASKETBALL_HALF_COURT", subFieldTypeLabels.BASKETBALL_HALF_COURT],
  ["BASKETBALL_FULL_COURT", subFieldTypeLabels.BASKETBALL_FULL_COURT],
  ["BADMINTON", subFieldTypeLabels.BADMINTON],
  ["VOLLEYBALL", subFieldTypeLabels.VOLLEYBALL],
  ["TENNIS", subFieldTypeLabels.TENNIS],
] as const;

export function formatFieldType(value: string | null | undefined) {
  if (!value) return "Chưa cập nhật";
  return fieldTypeLabels[value] ?? subFieldTypeLabels[value] ?? formatEnum(value);
}

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
  return `${sign}${formatted} đ`;
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
