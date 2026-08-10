export const API_STATUS_MESSAGES: Record<string, string> = {
  SUCCESS: "Thành công.",
  INTERNAL_ERROR: "Có lỗi đã xảy ra. Vui lòng thử lại sau.",
  INTERNAL_SERVER_ERROR: "Có lỗi đã xảy ra. Vui lòng thử lại sau.",

  UNAUTHORIZED: "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.",
  UNAUTHENTICATED: "Bạn cần đăng nhập để tiếp tục.",
  FORBIDDEN: "Bạn không có quyền thực hiện thao tác này.",
  RESOURCE_NOT_FOUND: "Không tìm thấy dữ liệu.",
  VALIDATION_ERROR: "Thông tin không hợp lệ.",
  INVALID_REQUEST: "Yêu cầu không hợp lệ.",
  INVALID_RESPONSE: "Phản hồi từ hệ thống không hợp lệ.",
  INVALID_ORIGIN: "Yêu cầu không được chấp nhận.",
  CONFLICT: "Dữ liệu đang bị xung đột.",
  DUPLICATE_REQUEST: "Thao tác này đã được thực hiện.",
  OPERATION_NOT_ALLOWED: "Không thể thực hiện thao tác này.",
  RATE_LIMITED: "Bạn thao tác quá nhanh. Vui lòng thử lại sau.",
  SERVICE_UNAVAILABLE: "Hệ thống đang tạm thời gián đoạn. Vui lòng thử lại sau.",

  USER_NOT_FOUND: "Không tìm thấy người dùng.",
  USER_PLATFORM_BANNED: "Bạn đã bị cấm đặt sân.",
  USER_FIELD_BANNED: "Bạn đã bị cấm đặt sân này.",
  USER_BANNED: "Người dùng đã bị cấm.",
  USER_UNBANNED: "Đã gỡ cấm người dùng.",
  USER_ROLE_UPDATED: "Đã cập nhật vai trò người dùng.",
  USER_STATUS_UPDATED: "Đã cập nhật trạng thái người dùng.",
  ACCOUNT_DISABLED: "Tài khoản đã bị vô hiệu hóa.",
  ACCOUNT_LOCKED: "Tài khoản đã bị khóa.",
  INVALID_CREDENTIALS: "Thông tin đăng nhập không hợp lệ.",
  INVALID_TOKEN: "Phiên đăng nhập không hợp lệ.",
  TOKEN_EXPIRED: "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.",

  FIELD_NOT_FOUND: "Không tìm thấy sân.",
  SUBFIELD_NOT_FOUND: "Không tìm thấy sân con.",
  FIELD_TYPE_NOT_FOUND: "Không tìm thấy loại sân.",
  FIELD_NOT_AVAILABLE: "Sân này hiện không thể đặt.",
  SUBFIELD_CLOSED: "Sân con đã đóng trong thời gian đã chọn.",
  FIELD_CREATED: "Đã tạo sân.",
  FIELD_UPDATED: "Đã cập nhật sân.",
  FIELD_STATUS_UPDATED: "Đã cập nhật trạng thái sân.",
  FIELD_TYPE_CREATED: "Đã tạo loại sân.",
  FIELD_TYPE_UPDATED: "Đã cập nhật loại sân.",
  FIELD_TYPE_DELETED: "Đã xóa loại sân.",
  FIELD_FAVORITE_ADDED: "Đã thêm sân vào danh sách yêu thích.",
  FIELD_FAVORITE_REMOVED: "Đã bỏ sân khỏi danh sách yêu thích.",
  FIELD_EMPLOYEE_ASSIGNED: "Đã phân công nhân viên.",
  FIELD_EMPLOYEE_REMOVED: "Đã gỡ phân công nhân viên.",
  FIELD_IMAGE_UPDATED: "Đã cập nhật hình ảnh sân.",
  FIELD_CLOSURE_CREATED: "Đã tạo lịch đóng sân.",
  FIELD_CLOSURE_UPDATED: "Đã cập nhật lịch đóng sân.",
  FIELD_CLOSURE_DELETED: "Đã xóa lịch đóng sân.",
  TIME_PRICE_RULES_OPERATING_HOURS_COVERAGE_REQUIRED: "Các khung giá phải bao phủ toàn bộ giờ hoạt động của sân.",
  REVIEW_CREATED: "Đã gửi đánh giá.",
  REVIEW_UPDATED: "Đã cập nhật đánh giá.",
  REVIEW_COMPLETED_BOOKING_REQUIRED: "Bạn cần hoàn tất một lượt đặt sân tại đây trước khi đánh giá.",

  BOOKING_NOT_AVAILABLE: "Khung giờ này không còn trống.",
  BOOKING_ALREADY_EXISTS: "Bạn đang có lịch đặt chưa xử lý.",
  BOOKING_NOT_FOUND: "Không tìm thấy lịch đặt sân.",
  BOOKING_CREATED: "Đặt sân thành công.",
  BOOKING_CANCELLED: "Đã hủy lịch đặt sân.",
  BOOKING_PAID: "Thanh toán lịch đặt sân thành công.",
  BOOKING_CANNOT_CANCEL: "Không thể hủy lịch đặt sân này.",
  BOOKING_CANNOT_MODIFY: "Không thể thay đổi lịch đặt sân này.",
  BOOKING_EXPIRED: "Lịch đặt sân đã hết hạn.",
  BOOKING_CONFLICT: "Khung giờ này không còn trống.",
  BOOKING_DATE_OUT_OF_RANGE: "Ngày đặt sân nằm ngoài khoảng cho phép.",
  RESERVATION_NOT_FOUND: "Không tìm thấy lịch giữ sân.",
  RESERVATION_CREATED: "Đã tạo lịch giữ sân.",
  RESERVATION_UPDATED: "Đã cập nhật lịch giữ sân.",
  RESERVATION_CANCELLED: "Đã hủy lịch giữ sân.",
  RESERVATION_CONFLICT: "Khung giờ này đã được giữ.",
  MATCH_RESULT_SAVED: "Đã lưu kết quả trận đấu.",

  INSUFFICIENT_BALANCE: "Số dư tài khoản không đủ.",
  PAYMENT_FAILED: "Thanh toán thất bại.",
  PAYMENT_NOT_FOUND: "Không tìm thấy giao dịch thanh toán.",
  PAYMENT_ALREADY_COMPLETED: "Giao dịch này đã hoàn tất.",
  PAYMENT_CHECKOUT_CREATED: "Đã tạo phiên thanh toán.",
  PAYMENT_COMPLETED: "Thanh toán thành công.",

  RECURRING_BOOKING_NOT_FOUND: "Không tìm thấy lịch đặt sân định kỳ.",
  RECURRING_BOOKING_PAUSED: "Đã tạm dừng lịch đặt sân định kỳ.",
  RECURRING_BOOKING_RESUMED: "Đã tiếp tục lịch đặt sân định kỳ.",
  RECURRING_BOOKING_CANCELLED: "Đã hủy lịch đặt sân định kỳ.",
  RECURRING_BOOKING_CREATED: "Đã tạo lịch đặt sân định kỳ.",
  RECURRING_BOOKING_UPDATED: "Đã cập nhật lịch đặt sân định kỳ.",
  RECURRING_BOOKING_ALREADY_PAUSED: "Lịch đặt sân định kỳ đã được tạm dừng.",
  RECURRING_BOOKING_ALREADY_ACTIVE: "Lịch đặt sân định kỳ đang hoạt động.",
  RECURRING_BOOKING_COMPLETED_BOOKING_REQUIRED: "Bạn cần hoàn tất ít nhất một lượt đặt sân tại sân này trước khi tạo lịch đặt định kỳ.",
  RECURRING_BOOKING_CONFLICT: "Lịch đặt sân định kỳ bị trùng với lịch hiện có.",
  RECURRING_SUBFIELD_CLOSED_ON_DATE: "Lịch đặt định kỳ có ngày trùng với lịch đóng sân.",

  POST_NOT_FOUND: "Không tìm thấy bài đăng.",
  POST_CREATED: "Đã tạo bài đăng.",
  POST_UPDATED: "Đã cập nhật bài đăng.",
  POST_CLOSED: "Đã đóng bài đăng.",
  POST_MARKED_FULL: "Đã đánh dấu bài đăng đủ người.",
  POST_REPORTED: "Đã báo cáo bài đăng.",
  POST_RESTORED: "Đã khôi phục bài đăng.",
  POST_ALREADY_REPORTED: "Bạn đã báo cáo bài đăng này.",
  POST_ALREADY_EXISTS: "Lịch đặt này đã có bài đăng.",
  POST_APPLICATION_SUBMITTED: "Đã gửi yêu cầu tham gia.",
  POST_APPLICATION_WITHDRAWN: "Đã rút yêu cầu tham gia.",
  POST_APPLICATION_ACCEPTED: "Đã chấp nhận yêu cầu tham gia.",
  POST_APPLICATION_REJECTED: "Đã từ chối yêu cầu tham gia.",
  POST_EVALUATION_SUBMITTED: "Đã gửi đánh giá trận đấu.",
  ALREADY_REPORTED: "Bạn đã gửi báo cáo này.",
  COMMUNITY_POSTING_RESTRICTED: "Bạn đang bị hạn chế đăng bài cộng đồng.",

  NO_SHOW_NOT_FOUND: "Không tìm thấy báo cáo vắng mặt.",
  NO_SHOW_REPORTED: "Đã báo cáo vắng mặt.",
  NO_SHOW_ALREADY_REPORTED: "Lịch đặt này đã được báo cáo vắng mặt.",

  PAYMENT_DISPUTE_NOT_FOUND: "Không tìm thấy báo cáo tranh chấp thanh toán.",
  PAYMENT_DISPUTE_REPORTED: "Đã gửi báo cáo tranh chấp thanh toán.",
  PAYMENT_DISPUTE_REVIEWED: "Đã xử lý báo cáo tranh chấp thanh toán.",
  PAYMENT_DISPUTE_ALREADY_REPORTED: "Lịch đặt này đã có báo cáo tranh chấp thanh toán.",
  PAYMENT_DISPUTE_ALREADY_REVIEWED: "Báo cáo tranh chấp này đã được xử lý.",

  NOTIFICATION_NOT_FOUND: "Không tìm thấy thông báo.",
  NOTIFICATION_MARKED_READ: "Đã đánh dấu thông báo là đã đọc.",
  NOTIFICATIONS_MARKED_READ: "Đã đánh dấu tất cả thông báo là đã đọc.",

  FILE_TOO_LARGE: "Tệp quá lớn.",
  UNSUPPORTED_FILE_TYPE: "Định dạng tệp không được hỗ trợ.",
  IMAGE_STORAGE_ERROR: "Không thể xử lý hình ảnh. Vui lòng thử lại sau.",
  UNKNOWN_ERROR: "Có lỗi đã xảy ra. Vui lòng thử lại sau.",
};

export const ERROR_MESSAGES = API_STATUS_MESSAGES;

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
      return status >= 500 ? "INTERNAL_ERROR" : "UNKNOWN_ERROR";
  }
}

export function getApiErrorMessage(statusCode?: string | null): string {
  return API_STATUS_MESSAGES[statusCode ?? "UNKNOWN_ERROR"] ?? API_STATUS_MESSAGES.UNKNOWN_ERROR;
}

export function localizedErrorMessage(statusCode?: string | null) {
  return getApiErrorMessage(statusCode);
}
