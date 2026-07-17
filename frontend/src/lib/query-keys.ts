export const fieldQueryKeys = {
  all: ["fields"] as const,
  list: (page: number, size = 9, status?: string) =>
    [...fieldQueryKeys.all, "list", { page, size, status }] as const,
  detail: (id: string) => [...fieldQueryKeys.all, id] as const,
  details: (id: string) => [...fieldQueryKeys.detail(id), "details"] as const,
  cards: (page: number, size = 9, filters: object = {}) =>
    [...fieldQueryKeys.all, "cards", { page, size, filters }] as const,
  favorites: ["fields", "favorites"] as const,
  operatingHours: (id: string) =>
    [...fieldQueryKeys.detail(id), "operating-hours"] as const,
  subFields: (id: string) => ["sub-fields", id] as const,
  reviews: (id: string) => ["reviews", id] as const,
};

export const bookingQueryKeys = {
  all: ["bookings"] as const,
  mine: (page: number, size = 10) =>
    [...bookingQueryKeys.all, "mine", { page, size }] as const,
  owner: (page: number, size = 10) =>
    [...bookingQueryKeys.all, "owner", { page, size }] as const,
  detail: (id: string) => [...bookingQueryKeys.all, id] as const,
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
  list: (page: number, size = 10) =>
    [...userQueryKeys.all, "list", { page, size }] as const,
};

export const notificationQueryKeys = {
  all: ["notifications"] as const,
  list: (page: number, size = 20) =>
    [...notificationQueryKeys.all, "list", { page, size }] as const,
  unreadCount: ["notifications", "unread-count"] as const,
};

export const fieldTypeQueryKeys = { all: ["field-types"] as const };

export const ownerFieldQueryKeys = {
  all: ["owner-fields"] as const,
  list: (page: number, size = 10) =>
    [...ownerFieldQueryKeys.all, "list", { page, size }] as const,
  closures: (fieldId: string, subFieldId: string) =>
    [...ownerFieldQueryKeys.all, fieldId, "closures", subFieldId] as const,
};

export const communityQueryKeys = {
  all: ["community-posts"] as const,
  list: (page: number, size = 10, filters: object = {}) =>
    [...communityQueryKeys.all, "list", { page, size, filters }] as const,
  detail: (id: string) => [...communityQueryKeys.all, id] as const,
  reports: (page: number, size = 20, status?: string) =>
    [...communityQueryKeys.all, "reports", { page, size, status }] as const,
};
