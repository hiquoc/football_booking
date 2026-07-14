export interface ApiResponse<T> {
  success: boolean;
  message: string | null;
  data: T;
  timestamp: string;
}

export interface ErrorResponse {
  code?: string | null;
  status: number;
  error: string;
  message: string;
  path?: string;
  timestamp?: string;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
  empty: boolean;
}

export interface FieldImage {
  id: number;
  imageUrl: string;
  isPrimary: boolean;
  displayOrder: number;
}

export interface FieldType {
  id: number;
  name: string;
  allowedSubFieldTypes: string[];
  defaultBookingDurationMinutes: number;
  active: boolean;
}

export interface Field {
  id: string;
  ownerId: string;
  name: string;
  description: string | null;
  address: string;
  ward: string;
  wardCode: string;
  province: string;
  provinceCode: string;
  legacyWard: string;
  legacyWardCode: string;
  legacyDistrict: string;
  legacyProvince: string;
  latitude: number;
  longitude: number;
  phoneNumber: string;
  email: string | null;
  active: boolean;
  status: FieldStatus;
  ratingAverage: number;
  totalReviews: number;
  isFavorite?: boolean;
  createdAt: string;
  updatedAt: string;
  images: FieldImage[];
  fieldTypes: FieldType[];
}

export interface ImageUploadSlot {
  imageId: number;
  publicId: string;
  timestamp: number;
  signature: string;
  apiKey: string;
  cloudName: string;
  uploadUrl: string;
  overwrite: false;
}

export interface CloudinaryUploadResult {
  public_id: string;
  secure_url: string;
  version: number;
  signature: string;
  format: string;
  width: number;
  height: number;
  bytes: number;
}

export interface FieldCardData {
  id: string;
  name: string;
  address: string;
  ward: string;
  province: string;
  latitude: number;
  longitude: number;
  ratingAverage: number;
  totalReviews: number;
  primaryImageUrl: string | null;
  fieldTypes: string[];
  distanceKm: number | null;
  isFavorite?: boolean;
}

export interface FieldCardFilters {
  keyword?: string;
  fieldType?: string;
  subFieldType?: string;
  district?: string;
  provinceCode?: string;
  latitude?: string;
  longitude?: string;
  radiusKm?: string;
  sortBy?: "rating" | "reviews" | "newest" | "distance";
  direction?: "asc" | "desc";
}

export interface FieldDetails {
  field: Field;
  operatingHours: OperatingHours[];
  subFields: SubField[];
  reviews: Review[];
}

export type FieldStatus = "PENDING" | "APPROVED" | "REJECTED";

export interface OperatingHours {
  id: string;
  fieldId: string | null;
  subFieldId: string | null;
  dayOfWeek: string;
  openTime: string | null;
  closeTime: string | null;
  closed: boolean;
}

export interface BookingRule {
  id: number;
  minimumBookingDurationMinutes: number;
  maximumBookingDurationMinutes: number;
  bookingIntervalMinutes: number;
}

export interface TimePriceRule {
  id: number;
  startTime: string;
  endTime: string;
  hourlyPrice: number;
}

export interface SubField {
  id: string;
  fieldId: string;
  fieldType: string;
  name: string;
  description: string | null;
  active: boolean;
  bookingDisabledFrom: string | null;
  indoorOutdoor: string | null;
  surfaceType: string | null;
  subFieldType: string;
  maxPlayers: number | null;
  lighting: boolean;
  parking: boolean;
  changingRoom: boolean;
  shower: boolean;
  wifi: boolean;
  airConditioning: boolean;
  bookingRule: BookingRule | null;
  timePriceRules: TimePriceRule[];
  createdAt: string;
  updatedAt: string;
}

export interface Review {
  id: string;
  fieldId: string;
  userId: string;
  rating: number;
  comment: string | null;
  createdAt: string;
}

export interface User {
  id: string;
  phoneNumber: string;
  email: string | null;
  fullName: string | null;
  avatarUrl: string | null;
  userType: "CLIENT" | "OWNER" | "ADMIN";
  status: string;
  balance: number;
  createdAt?: string;
  updatedAt?: string;
}

export type PaymentMethod = "STRIPE" | "ACCOUNT_BALANCE";

export type BookingStatus =
  | "PENDING"
  | "CONFIRMED"
  | "CANCELLED"
  | "COMPLETED"
  | "EXPIRED";

export interface Booking {
  id: string;
  bookingCode: string;
  clientId: string;
  subFieldId: string;
  subFieldName: string;
  fieldName: string;
  ownerId: string;
  bookingDate: string;
  startTime: string;
  endTime: string;
  durationMinutes: number;
  pricePerHour: number;
  totalAmount: number;
  paymentMethod?: PaymentMethod;
  status: BookingStatus;
  note: string | null;
  cancellationReason: string | null;
  cancelledAt: string | null;
  cancelledBy: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface Availability {
  openTime: string;
  closeTime: string;
  unavailableSlots: Array<{ startTime: string; endTime: string }>;
}

export interface CreateBookingInput {
  subFieldId: string;
  bookingDate: string;
  startTime: string;
  durationMinutes: number;
  note?: string;
  paymentMethod?: PaymentMethod;
}

export interface Notification {
  id: string;
  userId: string;
  code: string;
  title: string;
  payload: Record<string, unknown>;
  isRead: boolean;
  createdAt: string;
  readAt: string | null;
}

export interface FieldTypeInput {
  name: string;
  defaultBookingDurationMinutes: number;
  description?: string;
  active: boolean;
}

export interface FieldInput {
  name: string;
  description?: string;
  address: string;
  ward: string;
  wardCode: string;
  province: string;
  provinceCode: string;
  legacyWard: string;
  legacyWardCode: string;
  legacyDistrict: string;
  legacyProvince: string;
  latitude: number;
  longitude: number;
  phoneNumber: string;
  email?: string;
  active: boolean;
  operatingHours: Array<{
    dayOfWeek: string;
    openTime?: string;
    closeTime?: string;
    closed: boolean;
  }>;
}

export interface SubFieldInput {
  name: string;
  description?: string;
  active: boolean;
  subFieldType: string;
  bookingRule?: {
    minimumBookingDurationMinutes?: number;
    maximumBookingDurationMinutes?: number;
    bookingIntervalMinutes?: number;
  };
  timePriceRules?: Array<{
    startTime: string;
    endTime: string;
    hourlyPrice: number;
  }>;
}

export interface FieldClosure {
  id: string;
  subFieldId: string;
  startDate: string;
  endDate: string;
  reason: string;
}

export interface UpdateProfileInput {
  fullName?: string;
}

export type PaymentStatus = "PENDING" | "SUCCESS" | "FAILED" | "CANCELLED";
export type PaymentProvider = "STRIPE";
export interface Payment {
  id: string; bookingId: string; provider: PaymentProvider; amount: number;
  currency: string; status: PaymentStatus; failureReason: string | null;
  expiresAt: string | null;
  createdAt: string; updatedAt: string;
}
export interface CheckoutResponse { paymentId: string; checkoutUrl: string; }
export interface CreateCheckoutInput {
  bookingId: string; amount: number; currency: string; provider?: PaymentProvider;
}

export interface AvatarUploadSlot {
  publicId: string; timestamp: number; signature: string; apiKey: string;
  cloudName: string; uploadUrl: string; overwrite: false;
}

export interface BackendTokenResponse {
  accessToken: string;
  refreshToken: string;
}

export interface MutationSuccessResponse {
  success: boolean;
}

export interface FavoriteCheckResponse {
  favorite: boolean;
}
