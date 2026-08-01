import type {
  CommunityApplicationStatus,
  CommunityPostStatus,
  CommunityPostType,
  SkillLevel,
} from "@/lib/api/types";

export const postTypeLabels: Record<CommunityPostType, string> = {
  LOOKING_OPPONENT: "Tìm đối thủ",
  LOOKING_PLAYER: "Tìm thêm cầu thủ",
};

export const postStatusLabels: Record<CommunityPostStatus, string> = {
  OPEN: "Đang mở",
  MATCHED: "Đã ghép đội",
  FULL: "Đã đủ người",
  CLOSED: "Đã đóng",
  CANCELLED: "Đã hủy",
  HIDDEN: "Đã ẩn",
};

export const applicationStatusLabels: Record<CommunityApplicationStatus, string> = {
  PENDING: "Chờ duyệt",
  ACCEPTED: "Đã chấp nhận",
  REJECTED: "Đã từ chối",
  WITHDRAWN: "Đã rút",
};

export const skillLabels: Record<SkillLevel, string> = {
  VERY_WEAK: "Rất yếu",
  WEAK: "Yếu",
  AVERAGE: "Trung bình",
  ABOVE_AVERAGE: "Khá",
  GOOD: "Tốt",
  VERY_GOOD: "Rất tốt",
  SEMI_PRO: "Bán chuyên",
  PRO: "Chuyên nghiệp",
};

export const skillLevelOptions = Object.entries(skillLabels) as Array<[SkillLevel, string]>;

export function skillLabel(value: string | null | undefined) {
  return value && value in skillLabels ? skillLabels[value as SkillLevel] : value ?? "Chưa cập nhật";
}

export function timeRange(start: string, end: string) {
  return `${start.slice(0, 5)} - ${end.slice(0, 5)}`;
}
