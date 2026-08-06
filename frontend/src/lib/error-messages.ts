export const ERROR_MESSAGES: Record<string, string> = {
  FIELD_NOT_FOUND: "Không tìm thấy sân.",
  SUBFIELD_NOT_FOUND: "Không tìm thấy sân con.",
  FIELD_TYPE_NOT_FOUND: "Không tìm thấy loại sân.",
  RESOURCE_NOT_FOUND: "Không tìm thấy dữ liệu.",
  BOOKING_NOT_FOUND: "Không tìm thấy đơn đặt sân.",
  RESERVATION_NOT_FOUND: "Không tìm thấy lịch giữ sân.",
  RECURRING_BOOKING_NOT_FOUND: "Không tìm thấy lịch đặt sân định kỳ.",
  USER_NOT_FOUND: "Không tìm thấy người dùng.",
  PAYMENT_NOT_FOUND: "Không tìm thấy giao dịch.",
  NOTIFICATION_NOT_FOUND: "Không tìm thấy thông báo.",
  MATCH_POST_NOT_FOUND: "Không tìm thấy bài đăng.",
  BOOKING_CONFLICT: "Khung giờ này đã được đặt.",
  RESERVATION_CONFLICT: "Khung giờ này đã được giữ.",
  SUBFIELD_CLOSED: "Sân con đã đóng lịch vào ngày đã chọn.",
  CONFLICT: "Dữ liệu đang bị xung đột.",
  EMAIL_ALREADY_EXISTS: "Email đã được sử dụng.",
  PHONE_ALREADY_EXISTS: "Số điện thoại đã được sử dụng.",
  USERNAME_ALREADY_EXISTS: "Tên đăng nhập đã được sử dụng.",
  INSUFFICIENT_BALANCE: "Số dư không đủ.",
  PAYMENT_FAILED: "Thanh toán thất bại.",
  FILE_TOO_LARGE: "Tệp quá lớn.",
  UNSUPPORTED_FILE_TYPE: "Định dạng tệp không được hỗ trợ.",
  VALIDATION_ERROR: "Dữ liệu không hợp lệ.",
  INVALID_REQUEST: "Yêu cầu không hợp lệ.",
  UNAUTHORIZED: "Bạn cần đăng nhập.",
  UNAUTHENTICATED: "Bạn cần đăng nhập.",
  FORBIDDEN: "Bạn không có quyền thực hiện thao tác này.",
  INVALID_TOKEN: "Phiên đăng nhập không hợp lệ.",
  TOKEN_EXPIRED: "Phiên đăng nhập đã hết hạn.",
  ACCOUNT_DISABLED: "Tài khoản đã bị vô hiệu hóa.",
  ACCOUNT_LOCKED: "Tài khoản đã bị khóa.",
  INVALID_CREDENTIALS: "Thông tin đăng nhập không hợp lệ.",
  RATE_LIMITED: "Bạn thao tác quá nhanh. Vui lòng thử lại.",
  INTERNAL_SERVER_ERROR: "Đã xảy ra lỗi.",
  DATABASE_ERROR: "Đã xảy ra lỗi.",
  KAFKA_ERROR: "Đã xảy ra lỗi.",
  REDIS_ERROR: "Đã xảy ra lỗi.",
  SERVICE_UNAVAILABLE: "Hệ thống đang bảo trì.",
  UNKNOWN_ERROR: "Đã xảy ra lỗi.",
};

export function statusCodeFromPayload(payload: unknown): string {
  if (!payload || typeof payload !== "object") return "UNKNOWN_ERROR";
  if ("statusCode" in payload && typeof payload.statusCode === "string") {
    return payload.statusCode;
  }
  if ("code" in payload && typeof payload.code === "string") {
    return payload.code;
  }
  return "UNKNOWN_ERROR";
}

export function statusCodeFromHttpStatus(status: number): string {
  switch (status) {
    case 400:
      return "VALIDATION_ERROR";
    case 401:
      return "UNAUTHORIZED";
    case 403:
      return "FORBIDDEN";
    case 404:
      return "RESOURCE_NOT_FOUND";
    case 409:
      return "CONFLICT";
    case 429:
      return "RATE_LIMITED";
    case 503:
      return "SERVICE_UNAVAILABLE";
    default:
      return status >= 500 ? "INTERNAL_SERVER_ERROR" : "UNKNOWN_ERROR";
  }
}

export function localizedErrorMessage(statusCode?: string | null) {
  return ERROR_MESSAGES[statusCode ?? "UNKNOWN_ERROR"] ?? ERROR_MESSAGES.UNKNOWN_ERROR;
}

export function shouldUsePayloadMessage(statusCode?: string | null) {
  return statusCode === "RECURRING_SUBFIELD_CLOSED_ON_DATE";
}
