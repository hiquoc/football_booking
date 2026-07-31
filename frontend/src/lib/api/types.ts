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
  isSaved?: boolean;
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
  isSaved?: boolean;
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
  open24Hours: boolean;
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

export interface SubFieldFilterOption {
  id: string;
  name: string;
  fieldName: string | null;
  type: string | null;
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
  bio?: string | null;
  teamPhotoUrl?: string | null;
  skillLevel?: SkillLevel;
  totalMatches?: number;
  wins?: number;
  draws?: number;
  losses?: number;
  noCancelRate?: number;
  onTimeRate?: number;
  fairPlayRate?: number;
  userType: "CLIENT" | "OWNER" | "EMPLOYEE" | "ADMIN";
  status: string;
  balance: number;
  completedBookingCount?: number;
  isBookingBanned?: boolean;
  banExpiresAt?: string | null;
  isPermanentBan?: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export type SkillLevel =
  | "VERY_WEAK"
  | "WEAK"
  | "AVERAGE"
  | "ABOVE_AVERAGE"
  | "GOOD"
  | "VERY_GOOD"
  | "SEMI_PRO"
  | "PRO";

export interface PublicProfile {
  personal: {
    id: string;
    fullName: string | null;
    avatarUrl: string | null;
    phoneNumber: string | null;
    bio: string | null;
    teamPhotoUrl: string | null;
    skillLevel: SkillLevel;
  };
  statistics: {
    totalMatches: number;
    wins: number;
    draws: number;
    losses: number;
    winRate: number;
    completedBookingCount: number;
  };
  reputation: {
    noCancelRate: number;
    onTimeRate: number;
    fairPlayRate: number;
  };
  createdAt?: string;
  updatedAt?: string;
}

export type PaymentMethod = "ACCOUNT_BALANCE";

export type BookingStatus =
  | "PENDING"
  | "CONFIRMED"
  | "CANCELLED"
  | "COMPLETED"
  | "EXPIRED";

export type BookingPaymentStatus = "UNPAID" | "PAID" | "REFUNDED" | "FAILED";

export type MatchResultOutcome = "BOOKER_WIN" | "BOOKER_LOSS" | "DRAW";
export type WinningTeam = MatchResultOutcome | "TEAM_A" | "TEAM_B";

export interface MatchResult {
  id: string;
  bookingId: string;
  result?: MatchResultOutcome;
  winningTeam: WinningTeam;
  teamAPercentage: number;
  teamBPercentage: number;
  teamAAmount: number;
  teamBAmount: number;
  submittedBy: string;
  createdAt: string;
  updatedAt: string;
}

export interface MatchResultInput {
  result: MatchResultOutcome;
  teamAPercentage: number;
  teamBPercentage: number;
}

export interface Booking {
  id: string;
  bookingCode: string;
  clientId: string;
  clientName: string | null;
  clientPhoneNumber: string | null;
  clientAvatarUrl: string | null;
  subFieldId: string;
  subFieldName: string;
  fieldName: string;
  ownerId: string;
  bookingDate: string;
  startDateTime?: string;
  endDateTime?: string;
  startTime: string;
  endTime: string;
  durationMinutes: number;
  pricePerHour: number;
  totalAmount: number;
  subFieldPrice: number;
  bookingPrice: number;
  platformBookingFee: number;
  paymentMethod?: PaymentMethod;
  status: BookingStatus;
  paymentStatus: BookingPaymentStatus;
  note: string | null;
  cancellationReason: string | null;
  cancelledAt: string | null;
  paymentExpiresAt: string | null;
  cancelledBy: string | null;
  matchResult: MatchResult | null;
  createdAt: string;
  updatedAt: string;
}

export interface BookingConfig {
  id: string;
  firstBookingFee: number;
  notFirstBookingFee: number;
  refundBeforeHours: number;
  refundEnabled: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface Availability {
  openTime: string | null;
  closeTime: string | null;
  open24Hours: boolean;
  operatingHours?: Array<{
    date: string;
    openTime: string | null;
    closeTime: string | null;
    closed: boolean;
    open24Hours: boolean;
  }>;
  unavailableSlots: Array<{
    startTime: string;
    endTime: string;
    startDateTime?: string;
    endDateTime?: string;
  }>;
}

export interface CreateBookingInput {
  subFieldId: string;
  bookingDate?: string;
  startTime?: string;
  startDateTime: string;
  endDateTime: string;
  durationMinutes: number;
  note?: string;
  paymentMethod?: PaymentMethod;
}

export type RecurringBookingStatus = "ACTIVE" | "PAUSED" | "CANCELLED" | "COMPLETED";

export interface RecurringBooking {
  id: string;
  userId: string;
  fieldId: string;
  fieldName: string | null;
  subFieldId: string;
  subFieldName: string | null;
  startTime: string;
  endTime: string;
  startDate: string;
  endDate: string;
  intervalDays: number;
  status: RecurringBookingStatus;
  nextProcessAt: string | null;
  nextMatchAt?: string | null;
  firstBooking?: Booking | null;
  latestBooking?: Booking | null;
  createdAt: string;
  updatedAt: string;
}

export interface RecurringBookingInput {
  subFieldId: string;
  startTime: string;
  endTime: string;
  startDate: string;
  endDate: string;
  intervalDays: number;
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

export interface UserBalanceUpdateMessage {
  userId: string;
  balance: number;
  reason: string | null;
  occurredAt: string;
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
    open24Hours?: boolean;
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
  phoneNumber?: string;
  bio?: string | null;
  skillLevel?: SkillLevel;
}

export interface FieldEmployee {
  assignmentId: string;
  fieldId: string;
  employeeId: string;
  phoneNumber: string | null;
  fullName: string | null;
  email: string | null;
  avatarUrl: string | null;
  assignedAt: string;
}

export type PaymentStatus = "PENDING" | "SUCCESS" | "FAILED" | "CANCELLED";
export type PaymentProvider = "STRIPE";
export type PaymentPurpose = "WALLET_TOP_UP";
export interface Payment {
  id: string; bookingId: string | null; provider: PaymentProvider; purpose: PaymentPurpose; amount: number;
  currency: string; status: PaymentStatus; failureReason: string | null;
  expiresAt: string | null;
  createdAt: string; updatedAt: string;
}
export interface CheckoutResponse { paymentId: string; checkoutUrl: string; }
export interface CreateCheckoutInput {
  bookingId?: string; amount: number; currency: string; provider?: PaymentProvider;
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
  saved: boolean;
  favorite?: boolean;
}

export type CommunityPostType = "LOOKING_OPPONENT" | "LOOKING_PLAYER";
export type CommunityPostStatus = "OPEN" | "MATCHED" | "FULL" | "CLOSED" | "CANCELLED" | "HIDDEN";
export type CommunityApplicationStatus = "PENDING" | "ACCEPTED" | "REJECTED" | "WITHDRAWN";
export type CommunityReportReason = "SPAM" | "INAPPROPRIATE_CONTENT" | "HARASSMENT" | "FAKE_INFORMATION" | "SCAM" | "OTHER";
export type CommunityReportStatus = "PENDING" | "REVIEWED";
export type CommunityModerationAction =
  | "NO_ACTION"
  | "HIDE_POST"
  | "RESTORE_POST"
  | "ISSUE_WARNING"
  | "TEMPORARY_POSTING_BAN"
  | "PERMANENT_POSTING_BAN";

export interface CommunityApplication {
  id: string;
  postId: string;
  applicantId: string;
  status: CommunityApplicationStatus;
  message: string | null;
  applicantDisplayName: string | null;
  applicantAvatarUrl: string | null;
  applicantTeamPhotoUrl: string | null;
  applicantSkillLevel: SkillLevel | string | null;
  decidedAt: string | null;
  withdrawnAt: string | null;
  createdAt: string;
}

export interface CommunityPost {
  id: string;
  bookingId: string;
  ownerId: string;
  postType: CommunityPostType;
  status: CommunityPostStatus;
  title: string;
  description: string | null;
  skillLevel: SkillLevel | string;
  contactPhone: string;
  playersNeeded: number | null;
  acceptedPlayersCount: number;
  bookingCode: string;
  fieldId: string | null;
  fieldOwnerId: string | null;
  fieldName: string | null;
  subFieldId: string;
  subFieldName: string | null;
  fieldType: string | null;
  bookingDate: string;
  startTime: string;
  endTime: string;
  ownerDisplayName: string | null;
  ownerAvatarUrl: string | null;
  ownerTeamPhotoUrl: string | null;
  locationText: string | null;
  matchedApplicationId: string | null;
  closedAt: string | null;
  hiddenAt: string | null;
  hiddenReason: string | null;
  ownerUnderModeration: boolean | null;
  matchResultSubmitted?: boolean | null;
  ownerStatistics: CommunityPlayerStatistics | null;
  createdAt: string;
  updatedAt: string;
  applications: CommunityApplication[] | null;
}

export interface CommunityPlayerStatistics {
  totalMatches: number;
  winRate: number;
  onTimeRate: number;
  noCancelRate: number;
  fairPlayRate: number;
  completedBookingCount: number;
}

export interface MatchEvaluation {
  id: string;
  postId: string;
  bookingId: string;
  evaluatorId: string;
  evaluatedUserId: string;
  arrivedOnTime: boolean;
  cancelledUnexpectedly: boolean;
  fairPlay: boolean;
  wouldPlayAgain: boolean;
  comment: string | null;
  createdAt: string;
}

export interface MatchEvaluationInput {
  evaluatedUserId: string;
  arrivedOnTime: boolean;
  cancelledUnexpectedly: boolean;
  fairPlay: boolean;
  wouldPlayAgain: boolean;
  comment?: string;
}

export interface CommunityReport {
  id: string;
  postId: string;
  reporterId: string;
  reporterDisplayName?: string | null;
  reportedUserId?: string | null;
  reportedDisplayName?: string | null;
  reason: CommunityReportReason;
  description: string | null;
  status: CommunityReportStatus;
  reviewedBy: string | null;
  reviewedAt: string | null;
  createdAt: string;
  post: CommunityPost | null;
}

export interface CommunityViolation {
  id: string;
  userId: string;
  reason: string;
  action: CommunityModerationAction;
  expireAt: string | null;
  status: "ACTIVE" | "EXPIRED" | "PERMANENT";
  sourcePostId: string | null;
  createdAt: string;
}

export interface UserViolationHistory {
  community: PageResponse<CommunityViolation>;
  field: PageResponse<FieldViolation>;
}

export interface CommunityModerationHistory {
  id: string;
  targetUserId: string;
  targetPostId: string | null;
  moderatorId: string;
  action: string;
  reason: string;
  note: string | null;
  createdAt: string;
}

export interface AdminModerationInput {
  action: CommunityModerationAction;
  targetUserId?: string;
  targetPostId?: string;
  reason: string;
  note?: string;
  expireAt?: string;
}

export interface CommunityPostFilters {
  ownerId?: string;
  applicantId?: string;
  postType?: CommunityPostType;
  skillLevel?: SkillLevel | string;
  date?: string;
  fieldType?: string;
  city?: string;
  district?: string;
  fieldName?: string;
  status?: CommunityPostStatus | "all";
  keyword?: string;
  sortBy?: "newest" | "upcoming";
}

export interface CreateCommunityPostInput {
  bookingId: string;
  postType: CommunityPostType;
  title: string;
  description?: string;
  skillLevel: SkillLevel | string;
  contactPhone: string;
  playersNeeded?: number;
  ownerDisplayName?: string | null;
  ownerAvatarUrl?: string | null;
  ownerTeamPhotoUrl?: string | null;
}

export interface FieldViolation {
  id: string;
  userId: string;
  userDisplayName?: string | null;
  fieldId: string;
  violationCount: number;
  banned: boolean;
  banDate: string | null;
  lastViolationDate: string | null;
  createdAt: string;
  updatedAt: string;
}

export type PaymentDisputeStatus = "PENDING" | "APPROVED" | "REJECTED";

export interface PaymentDisputeReport {
  id: string;
  bookingId: string;
  fieldId: string;
  reportedUserId: string;
  ownerId: string;
  description: string;
  status: PaymentDisputeStatus;
  adminNote: string | null;
  imageUrls: string[];
  createdAt: string;
  reviewedAt: string | null;
  reviewedBy: string | null;
}

export interface CreatePaymentDisputeInput {
  bookingId: string;
  description: string;
  imageUrls: string[];
}

export interface UpdateCommunityPostInput {
  title: string;
  description?: string;
  skillLevel: SkillLevel | string;
  contactPhone: string;
  playersNeeded?: number;
}
