export const fieldQueryKeys = {
  all: ["fields"] as const,
  list: (page: number, size = 9, status?: string) =>
    [...fieldQueryKeys.all, "list", { page, size, status }] as const,
  detail: (id: string) => [...fieldQueryKeys.all, id] as const,
  details: (id: string) => [...fieldQueryKeys.detail(id), "details"] as const,
  cards: (page: number, size = 9, filters: object = {}) =>
    [...fieldQueryKeys.all, "cards", { page, size, filters }] as const,
  favorites: (page: number, size = 4) =>
    [...fieldQueryKeys.all, "favorites", { page, size }] as const,
  operatingHours: (id: string) =>
    [...fieldQueryKeys.detail(id), "operating-hours"] as const,
  subFields: (id: string) => ["sub-fields", id] as const,
  adminSearch: (keyword = "") =>
    ["field-search", keyword] as const,
  subFieldFilterOptions: (search = "") =>
    ["sub-fields", "filter-options", { search }] as const,
  reviews: (id: string) => ["reviews", id] as const,
  reviewPage: (id: string, page: number, size: number) =>
    [...fieldQueryKeys.reviews(id), { page, size }] as const,
  myReview: (id: string) => [...fieldQueryKeys.reviews(id), "me"] as const,
};

export const bookingQueryKeys = {
  all: ["bookings"] as const,
  mine: (page: number, size = 10, filters: object = {}) =>
    [...bookingQueryKeys.all, "mine", { page, size, filters }] as const,
  owner: (page: number, size = 10, filters: object = {}) =>
    [...bookingQueryKeys.all, "owner", { page, size, filters }] as const,
  reservations: (page: number, size = 10, filters: object = {}) =>
    [...bookingQueryKeys.all, "reservations", { page, size, filters }] as const,
  detail: (id: string) => [...bookingQueryKeys.all, id] as const,
  config: ["bookings", "config"] as const,
  availability: (subFieldId: string, date: string) =>
    ["availability", subFieldId, date] as const,
};

export const recurringBookingQueryKeys = {
  all: ["recurring-bookings"] as const,
  mine: (page: number, size = 10, status?: string) =>
    [...recurringBookingQueryKeys.all, "mine", { page, size, status }] as const,
  owner: (page: number, size = 10, status?: string) =>
    [...recurringBookingQueryKeys.all, "owner", { page, size, status }] as const,
  admin: (page: number, size = 10, status?: string) =>
    [...recurringBookingQueryKeys.all, "admin", { page, size, status }] as const,
};

export const paymentQueryKeys = {
  all: ["payments"] as const,
  byBooking: (bookingId: string) => [...paymentQueryKeys.all, bookingId] as const,
};

export const userQueryKeys = {
  all: ["users"] as const,
  me: ["user", "me"] as const,
  mePrivate: ["user", "me", "private"] as const,
  profile: (id: string) => [...userQueryKeys.all, "profile", id] as const,
  list: (page: number, size = 10, phoneNumber = "") =>
    [...userQueryKeys.all, "list", { page, size, phoneNumber }] as const,
  violations: (id: string) => [...userQueryKeys.all, "violations", id] as const,
};

export const notificationQueryKeys = {
  all: ["notifications"] as const,
  list: (page: number, size = 20) =>
    [...notificationQueryKeys.all, "list", { page, size }] as const,
  unreadCount: ["notifications", "unread-count"] as const,
};

export const fieldTypeQueryKeys = {
  all: ["field-types"] as const,
  subFieldTypes: ["sub-field-types"] as const,
};

export const ownerFieldQueryKeys = {
  all: ["owner-fields"] as const,
  managed: (page: number, size = 10) =>
    [...ownerFieldQueryKeys.all, "managed", { page, size }] as const,
  list: (page: number, size = 10) =>
    [...ownerFieldQueryKeys.all, "list", { page, size }] as const,
  assigned: (page: number, size = 10) =>
    [...ownerFieldQueryKeys.all, "assigned", { page, size }] as const,
  employees: (fieldId: string) => [...ownerFieldQueryKeys.all, fieldId, "employees"] as const,
  closures: (fieldId: string, subFieldId: string) =>
    [...ownerFieldQueryKeys.all, fieldId, "closures", subFieldId] as const,
};

export const communityQueryKeys = {
  all: ["community-posts"] as const,
  list: (page: number, size = 10, filters: object = {}) =>
    [...communityQueryKeys.all, "list", { page, size, filters }] as const,
  detail: (id: string) => [...communityQueryKeys.all, id] as const,
  evaluations: (id: string) => [...communityQueryKeys.detail(id), "evaluations"] as const,
  reports: (page: number, size = 20, status?: string) =>
    [...communityQueryKeys.all, "reports", { page, size, status }] as const,
};

export const moderationQueryKeys = {
  all: ["moderation"] as const,
  fieldViolations: (fieldId: string, page: number, size = 20) =>
    [...moderationQueryKeys.all, "field-violations", { fieldId, page, size }] as const,
  noShowReports: (fieldId: string, page: number, size = 20) =>
    [...moderationQueryKeys.all, "no-show-reports", { fieldId, page, size }] as const,
  auditLogs: (fieldId: string, page: number, size = 20) =>
    [...moderationQueryKeys.all, "audit-logs", { fieldId, page, size }] as const,
  adminPaymentDisputes: (page: number, size = 20, filters: object = {}) =>
    [...moderationQueryKeys.all, "admin-payment-disputes", { page, size, filters }] as const,
};
