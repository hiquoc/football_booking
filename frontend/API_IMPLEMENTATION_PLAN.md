# Football Booking System - Frontend Implementation Plan

## Overview
This document outlines the backend API surface and the production-oriented plan for building the Next.js frontend.

### Status and constraints

- The browser communicates with a single same-origin Next.js Backend-for-Frontend (BFF). The BFF communicates with the API gateway; the browser must not call individual microservices directly.
- Authentication and authorization are always enforced by the gateway and services. Frontend route checks are user-experience optimizations, not a security boundary.
- The current development OTP remains fixed at `111111` by project decision. This must be controlled by an explicit environment setting and must never be presented as real OTP security. Before a public production launch, replace it with a generated OTP and an SMS provider.
- Sections marked **Backend prerequisite** describe APIs or gateway routes that do not exist yet and block the corresponding frontend feature.

---

## 1. Project Setup

### Tech Stack
- **Framework**: Next.js 16 (App Router), pinned to a tested minor/patch version with a committed lockfile
- **Language**: TypeScript with strict mode
- **Rendering/Data**: Server Components and native `fetch` for initial/public data; TanStack Query only for interactive client state, polling, and mutations
- **API boundary**: Same-origin Next.js Route Handlers/Server Actions acting as a BFF to the API gateway
- **Authentication**: Tokens handled only by the BFF in `HttpOnly`, `Secure`, explicit `SameSite` cookies; never `localStorage`, session storage, or a client Zustand store
- **UI Library**: Tailwind CSS + shadcn/ui (or similar component library)
- **Form Validation**: React Hook Form + Zod
- **Testing**: Vitest/Jest + Testing Library for units/components, MSW for API boundaries, and Playwright for critical user journeys
- **Contracts**: Generate TypeScript schemas/types from the backend OpenAPI documents and validate external responses at the BFF boundary

### Project Structure
```
frontend/
├── src/
│   ├── app/                    # Next.js App Router pages
│   │   ├── auth/               # Login/OTP pages
│   │   ├── (main)/             # Authenticated layout
│   │   │   ├── fields/         # Field listing & detail
│   │   │   ├── bookings/       # Booking management
│   │   │   ├── profile/        # User profile
│   │   │   ├── owner/          # Owner dashboard
│   │   │   └── admin/          # Admin panel
│   │   └── layout.tsx
│   ├── components/             # Shared UI components
│   │   ├── ui/                 # Base UI components
│   │   ├── fields/             # Field-related components
│   │   ├── bookings/           # Booking-related components
│   │   └── layout/             # Layout components (Navbar, Sidebar, etc.)
│   ├── lib/                    # Utilities
│   │   ├── api/                # Generated contracts and shared API types
│   │   ├── server/             # Server-only gateway client, auth/session, DAL
│   │   ├── client/             # Browser client for same-origin BFF endpoints
│   │   ├── hooks/              # Custom hooks
│   │   ├── types/              # TypeScript interfaces
│   │   └── utils/              # Helper functions
│   └── proxy.ts                # Optimistic cookie-based redirects only
├── public/
└── package.json
```

---

## 2. Authentication & User Service APIs (`/api/v1/auth`, `/api/v1/users`)

### 2.1 Auth Endpoints

| Method | Endpoint | Auth | Description | Frontend Page/Component |
|--------|----------|------|-------------|------------------------|
| POST | `/api/v1/auth/otp/send` | No | Send OTP to phone number | `LoginPage` - Phone input step |
| POST | `/api/v1/auth/otp/verify` | No | Verify OTP; BFF consumes the token pair and creates the browser session | `LoginPage` Server Action/Route Handler |
| POST | `/api/v1/auth/refresh` | Server cookie | BFF refreshes the backend access token; never expose the refresh token to browser JavaScript | Server-only gateway client |
| POST | `/api/v1/auth/logout` | Server cookie | Invalidate refresh token and clear all browser session cookies | `LogoutButton` Server Action |

### 2.2 User Endpoints

| Method | Endpoint | Auth | Description | Frontend Page/Component |
|--------|----------|------|-------------|------------------------|
| GET | `/api/v1/users/me` | JWT | Get current user profile | `ProfilePage`, global auth context |
| GET | `/api/v1/users/{id}` | JWT | Get user by ID | `UserProfilePage` |
| PATCH | `/api/v1/users/me` | JWT | Update profile (fullName, avatarUrl) | `ProfileEditPage` |
| PUT | `/api/v1/users/{id}/role` | ADMIN | Change user role | `AdminUserManagementPage` |

### 2.3 Request/Response Types

```typescript
// ---- AUTH ----
// POST /api/v1/auth/otp/send
interface SendOtpRequest {
  phoneNumber: string; // "0862470050" or "+84999999999"
}

// POST /api/v1/auth/otp/verify
interface VerifyOtpRequest {
  phoneNumber: string;
  code: string; // "111111"
}
// Server-only backend response. Never return this object from the BFF to the browser.
interface BackendTokenResponse {
  accessToken: string;
  refreshToken: string;
}

// Browser requests to the same-origin BFF send no token in the body.
type RefreshSessionRequest = Record<string, never>;
type LogoutRequest = Record<string, never>;

// ---- USER ----
interface UserDto {
  id: string; // UUID
  phoneNumber: string;
  email: string | null;
  fullName: string | null;
  avatarUrl: string | null;
  userType: "CLIENT" | "OWNER" | "ADMIN";
  socialProvider: string | null;
  socialProviderId: string | null;
  status: string; // "ACTIVE"
  createdAt: string; // ISO datetime
  updatedAt: string; // ISO datetime
}

// PATCH /api/v1/users/me
interface UpdateProfileRequest {
  fullName?: string;
  avatarUrl?: string;
}

// PUT /api/v1/users/{id}/role
interface ChangeRoleRequest {
  userType: "CLIENT" | "OWNER" | "ADMIN";
}
```

### 2.4 Production Authentication Design

- **Auth flow**: Browser submits phone and OTP to a same-origin Server Action/Route Handler. The BFF calls the gateway, consumes `BackendTokenResponse`, and stores the session in server-readable `HttpOnly`, `Secure`, explicit `SameSite` cookies.
- **Token exposure**: Do not return either token to client JavaScript and do not persist auth tokens in Zustand, `localStorage`, or session storage.
- **Refresh**: The server-only gateway client may perform one refresh and retry after a 401. Use a single-flight refresh guard and a retry marker to prevent refresh storms and infinite loops.
- **Route checks**: `proxy.ts` may redirect based on a signed/encrypted session cookie. Protected layouts/pages must also check the session through a server-only data-access layer. Backend authorization remains authoritative.
- **Roles**: Protect `/owner/*` and `/admin/*` in the server layout/data layer and show a 403 state when the backend rejects an operation.
- **CSRF**: Mutating BFF endpoints must validate `Origin`/`Host`; use `SameSite=Lax` or stricter cookies and a CSRF token where cross-site flows require looser cookie policy.
- **Backend prerequisite**: Stop exposing refresh tokens in JSON, rotate refresh tokens on use, and detect refresh-token reuse. Until that backend change is made, the BFF must consume and suppress the current JSON `refreshToken` field.

---

## 3. Field Service APIs

### 3.1 Fields (`/api/v1/fields`)

| Method | Endpoint | Auth | Description | Frontend Page/Component |
|--------|----------|------|-------------|------------------------|
| POST | `/api/v1/fields` | OWNER | Create a new field | `OwnerFieldCreatePage` |
| PUT | `/api/v1/fields/{id}` | OWNER | Update field | `OwnerFieldEditPage` |
| GET | `/api/v1/fields/{id}` | No | Get field by ID | `FieldDetailPage` |
| GET | `/api/v1/fields` | No | Get all fields (paginated) | `FieldListPage`, `HomePage` |
| GET | `/api/v1/fields/owner` | OWNER | Get every field owned by the current user (paginated, including inactive and non-approved fields) | `OwnerFieldsPage` |
| GET | `/api/v1/fields/{id}/operating-hours` | No | Get field operating hours | `FieldDetailPage` |
| PUT | `/api/v1/fields/{id}/operating-hours` | ADMIN/OWNER | Replace operating hours | `OwnerFieldEditPage` |

**Query Parameters for GET `/api/v1/fields`**:
- `page` (int), `size` (int), `sort` (string)

**Query Parameters for GET `/api/v1/fields/owner`**:
- `page` (int), `size` (int), `sort` (string); the frontend defaults to `createdAt,desc`

### 3.2 Field Images (`/api/v1/fields/{fieldId}/images`)

| Method | Endpoint | Auth | Description | Frontend Page/Component |
|--------|----------|------|-------------|------------------------|
| POST | `/api/v1/fields/{fieldId}/images` | OWNER | Upload images (multipart) | `OwnerFieldImageUpload` |
| PUT | `/api/v1/fields/{fieldId}/images/order` | OWNER | Order & set primary image | `OwnerFieldImageManager` |
| DELETE | `/api/v1/fields/{fieldId}/images/{imageId}` | OWNER | Delete image | `OwnerFieldImageManager` |

### 3.3 Field Types (`/api/v1/field-types`)

| Method | Endpoint | Auth | Description | Frontend Page/Component |
|--------|----------|------|-------------|------------------------|
| POST | `/api/v1/field-types` | ADMIN | Create field type | `AdminFieldTypePage` |
| PUT | `/api/v1/field-types/{id}` | ADMIN | Update field type | `AdminFieldTypePage` |
| DELETE | `/api/v1/field-types/{id}` | ADMIN | Delete field type | `AdminFieldTypePage` |
| GET | `/api/v1/field-types` | No | Get all field types | `FieldCreateForm` (dropdown) |

### 3.4 Sub-Fields (`/api/v1/sub-fields`)

| Method | Endpoint | Auth | Description | Frontend Page/Component |
|--------|----------|------|-------------|------------------------|
| POST | `/api/v1/sub-fields/field/{fieldId}` | ADMIN/OWNER | Create sub-field | `OwnerSubFieldCreatePage` |
| GET | `/api/v1/sub-fields/field/{fieldId}` | No | Get all sub-fields for field | `FieldDetailPage` |
| PUT | `/api/v1/sub-fields/{id}` | ADMIN/OWNER | Update sub-field | `OwnerSubFieldEditPage` |
| DELETE | `/api/v1/sub-fields/{id}` | ADMIN/OWNER | Delete sub-field | `OwnerSubFieldManager` |
| GET | `/api/v1/sub-fields/{id}/operating-hours` | No | Get sub-field hours | `SubFieldDetail` |
| PUT | `/api/v1/sub-fields/{id}/operating-hours` | ADMIN/OWNER | Replace sub-field hours | `OwnerSubFieldEditPage` |
| GET | `/api/v1/sub-fields/{id}/closures` | No | Get sub-field closures | `SubFieldSchedule` |
| POST | `/api/v1/sub-fields/closures` | ADMIN/OWNER | Create closures | `OwnerClosureManager` |
| PUT | `/api/v1/sub-fields/closures/{closureId}` | ADMIN/OWNER | Update closure | `OwnerClosureManager` |
| DELETE | `/api/v1/sub-fields/closures/{closureId}` | ADMIN/OWNER | Delete closure | `OwnerClosureManager` |

### 3.5 Reviews (`/api/v1/reviews`)

| Method | Endpoint | Auth | Description | Frontend Page/Component |
|--------|----------|------|-------------|------------------------|
| POST | `/api/v1/reviews` | CLIENT | Submit review | `ReviewForm` (on FieldDetailPage) |
| GET | `/api/v1/reviews/field/{fieldId}` | No | Get reviews for field | `FieldDetailPage` - Reviews section |

### 3.6 Internal (`/api/v1/internal/sub-fields`)

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| GET | `/api/v1/internal/sub-fields/{subFieldId}` | Internal | Internal sub-field lookup (not used by frontend directly) |

### 3.7 Gateway prerequisites

These must be corrected before the frontend uses the gateway as its single production ingress:

- Permit the intended public `GET` routes for fields, field types, reviews, sub-fields, and booking availability. The gateway currently authenticates every route except `/api/v1/auth/**`.
- Change the gateway route from `/api/v1/subfields/**` to `/api/v1/sub-fields/**`.
- Add gateway routes for `/api/v1/field-types/**` and `/api/v1/reviews/**`.
- Do not expose `/api/v1/internal/**` through the public gateway.
- Configure an explicit frontend/BFF origin policy. Do not use wildcard origins with credentials or WebSockets.
- Keep service URLs private; only the BFF/gateway ingress is public.

### 3.8 Request/Response Types

The interfaces below are illustrative domain shapes. Generated OpenAPI types are authoritative and should replace hand-maintained duplicates in application code.

```typescript
// ---- FIELD ----
interface FieldDto {
  id: string;
  ownerId: string;
  name: string;
  description: string | null;
  address: string;
  latitude: number;
  longitude: number;
  phoneNumber: string;
  email: string | null;
  active: boolean;
  status: "PENDING" | "APPROVED" | "REJECTED";
  ratingAverage: number;
  totalReviews: number;
  createdAt: string;
  updatedAt: string;
  images: FieldImageDto[];
  fieldTypes: FieldTypeDto[];
}

interface FieldRequest {
  name: string;
  description?: string;
  address: string;
  latitude: number;
  longitude: number;
  phoneNumber: string;
  email?: string;
  operatingHours: OperatingHoursRequest[];
  active: boolean;
}

interface OperatingHoursRequest {
  dayOfWeek: "MONDAY" | "TUESDAY" | "WEDNESDAY" | "THURSDAY" | "FRIDAY" | "SATURDAY" | "SUNDAY";
  openTime?: string; // "HH:mm:ss" - required if closed=false
  closeTime?: string; // "HH:mm:ss" - required if closed=false
  closed: boolean;
}

interface OperatingHoursDto {
  id: number;
  dayOfWeek: string;
  openTime: string | null;
  closeTime: string | null;
  closed: boolean;
}

// ---- FIELD IMAGE ----
interface FieldImageDto {
  id: number;
  imageUrl: string;
  isPrimary: boolean;
  displayOrder: number;
}

interface FieldImageOrderRequest {
  imageIds: number[];
  primaryImageId: number;
}

// ---- FIELD TYPE ----
interface FieldTypeDto {
  id: number;
  name: string; // SportType enum
  allowedSubFieldTypes: string[];
  defaultBookingDurationMinutes: number;
  active: boolean;
}

interface FieldTypeRequest {
  name: string;
  defaultBookingDurationMinutes: number;
  description?: string;
  active: boolean;
}

// ---- SUB-FIELD ----
interface SubFieldDto {
  id: string;
  fieldId: string;
  name: string;
  description: string | null;
  subFieldType: string; // e.g. "FOOTBALL_5V5"
  active: boolean;
  bookingRule: BookingRuleDto;
  timePriceRules: TimePriceRuleDto[];
  createdAt: string;
  updatedAt: string;
}

interface SubFieldRequest {
  name: string;
  description?: string;
  active: boolean;
  subFieldType: string;
  bookingRule?: {
    minimumBookingDurationMinutes?: number;
    maximumBookingDurationMinutes?: number;
    bookingIntervalMinutes?: number;
  };
  timePriceRules?: TimePriceRuleDto[];
}

interface BookingRuleDto {
  minimumBookingDurationMinutes: number;
  maximumBookingDurationMinutes: number;
  bookingIntervalMinutes: number;
}

interface TimePriceRuleDto {
  startTime: string;
  endTime: string;
  hourlyPrice: number;
}

// ---- FIELD CLOSURE ----
interface FieldClosureDto {
  id: string;
  subFieldId: string;
  startDate: string;
  endDate: string;
  reason: string;
}

interface FieldClosureRequest {
  subFieldIds: string[];
  startDate: string;
  endDate: string;
  reason: string;
}

// ---- REVIEW ----
interface ReviewDto {
  id: string;
  fieldId: string;
  userId: string;
  rating: number;
  comment: string | null;
  createdAt: string;
}

interface ReviewRequest {
  fieldId: string;
  rating: number;
  comment?: string;
}
```

---

## 4. Booking Service APIs (`/api/v1/bookings`)

| Method | Endpoint | Auth | Description | Frontend Page/Component |
|--------|----------|------|-------------|------------------------|
| POST | `/api/v1/bookings` | CLIENT | Create booking | `BookingCreatePage` |
| PATCH | `/api/v1/bookings/cancel` | CLIENT | Cancel booking (by client) | `BookingDetailPage` |
| PATCH | `/api/v1/bookings/owner/cancel` | OWNER | Cancel booking (by owner) | `OwnerBookingDetailPage` |
| GET | `/api/v1/bookings/my` | CLIENT | Get my bookings (paginated) | `MyBookingsPage` |
| GET | `/api/v1/bookings/owner` | OWNER | Get owner's bookings (paginated) | `OwnerBookingsPage` |
| GET | `/api/v1/bookings/{bookingId}` | CLIENT | Get booking by ID | `BookingDetailPage` |
| GET | `/api/v1/bookings/availability` | No | Check availability | `BookingCreatePage` (time slot picker) |

## 5. Payment Service APIs (`/api/v1/payments`)

| Method | Endpoint | Auth | Description | Frontend Page/Component |
|--------|----------|------|-------------|------------------------|
| POST | `/api/v1/payments/checkout` | CLIENT | Create Stripe Checkout Session | `BookingPaymentPage` |
| GET | `/api/v1/payments/{bookingId}` | CLIENT | Get verified payment status | `BookingPaymentPage` |
| POST | `/api/v1/payments/webhook` | Stripe signature | Process Stripe webhook | Stripe |

**Query Parameters**:
- `GET /availability`: `subFieldId` (UUID), `date` (ISO date)
- `GET /my`: `page`, `size`, `sort`
- `GET /owner`: `page`, `size`, `sort`

### Request/Response Types

```typescript
// ---- BOOKING ----
interface CreateBookingRequest {
  subFieldId: string;
  bookingDate: string; // "2025-06-20"
  startTime: string;   // "08:00:00"
  durationMinutes: number; // 120
  note?: string;
}

interface CancelBookingRequest {
  bookingId: string;
  reason?: string;
}

interface BookingResponse {
  id: string;
  bookingCode: string; // "BK-20250613-0001"
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
  status: "PENDING" | "CONFIRMED" | "CANCELLED" | "COMPLETED";
  note: string | null;
  cancellationReason: string | null;
  cancelledAt: string | null;
  cancelledBy: string | null;
  createdAt: string;
  updatedAt: string;
}

// ---- AVAILABILITY ----
interface AvailabilityResponse {
  openTime: string;     // "06:00:00"
  closeTime: string;    // "23:00:00"
  unavailableSlots: UnavailableSlotResponse[];
}

interface UnavailableSlotResponse {
  startTime: string;
  endTime: string;
}
```

### Booking Status Flow
```
PENDING → CONFIRMED (after mock payment)
PENDING → CANCELLED
CONFIRMED → CANCELLED
```

---

## 5. Notification Service APIs (`/api/v1/notifications`)

| Method | Endpoint | Auth | Description | Frontend Page/Component |
|--------|----------|------|-------------|------------------------|
| GET | `/api/v1/notifications` | JWT | Get notifications (paginated) | `NotificationPage` |
| GET | `/api/v1/notifications/unread` | JWT | Get unread notifications | `NotificationBell` (Navbar) |
| PATCH | `/api/v1/notifications/{id}/read` | JWT | Mark notification as read | `NotificationItem` |
| PATCH | `/api/v1/notifications/read-all` | JWT | Mark all as read | `NotificationHeader` |
| GET | `/api/v1/notifications/unread-count` | JWT | Count unread notifications | `NotificationBadge` (Navbar) |

### Request/Response Types

```typescript
interface NotificationResponse {
  id: string;
  userId: string;
  code: string; // "BOOKING_CONFIRMED", "BOOKING_CANCELLED", "PAYMENT_SUCCESS"
  title: string;
  payload: Record<string, any>;
  isRead: boolean;
  createdAt: string;
  readAt: string | null;
}

interface NotificationSummaryResponse {
  count: number;
}
```

---

## 6. Common Types & API Response Wrapper

```typescript
// All API responses are wrapped in this
interface ApiResponse<T> {
  success: boolean;
  message: string | null;
  data: T;
}

// Paginated responses use this structure
interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
  empty: boolean;
}
```

---

## 7. Frontend Pages & Features Summary

### Public Pages
| Page | Route | APIs Required |
|------|-------|---------------|
| Home | `/` | `GET /api/v1/fields` |
| Field List | `/fields` | `GET /api/v1/fields` |
| Field Detail | `/fields/{id}` | `GET /api/v1/fields/{id}`, `GET /api/v1/fields/{id}/operating-hours`, `GET /api/v1/reviews/field/{fieldId}`, `GET /api/v1/sub-fields/field/{fieldId}` |
| Login | `/auth/login` | `POST /api/v1/auth/otp/send`, `POST /api/v1/auth/otp/verify` |

### Client Pages
| Page | Route | APIs Required |
|------|-------|---------------|
| My Bookings | `/bookings` | `GET /api/v1/bookings/my` |
| Booking Detail | `/bookings/{id}` | `GET /api/v1/bookings/{bookingId}` |
| Create Booking | `/fields/{id}/book` | `GET /api/v1/bookings/availability`, `POST /api/v1/bookings` |
| Payment | `/bookings/{id}/payment` | `POST /api/v1/payments/checkout`, `GET /api/v1/payments/{bookingId}` |
| Profile | `/profile` | `GET /api/v1/users/me` |
| Edit Profile | `/profile/edit` | `PATCH /api/v1/users/me` |

### Owner Pages
| Page | Route | APIs Required |
|------|-------|---------------|
| Dashboard | `/owner` | `GET /api/v1/bookings/owner` |
| My Fields | `/owner/fields` | `GET /api/v1/fields/owner` |
| Create Field | `/owner/fields/new` | `POST /api/v1/fields`, `GET /api/v1/field-types` |
| Edit Field | `/owner/fields/{id}/edit` | `GET /api/v1/fields/{id}`, `PUT /api/v1/fields/{id}`, `PUT /api/v1/fields/{id}/operating-hours` |
| Manage Sub-Fields | `/owner/fields/{id}/sub-fields` | `GET /api/v1/sub-fields/field/{fieldId}`, `POST/DELETE/PUT` |
| Manage Images | `/owner/fields/{id}/images` | `POST /api/v1/fields/{fieldId}/images`, `PUT order`, `DELETE` |
| Manage Closures | `/owner/fields/{id}/closures` | `GET/POST/PUT/DELETE /api/v1/sub-fields/closures*` |
| Owner Bookings | `/owner/bookings` | `GET /api/v1/bookings/owner` |

### Admin Pages
| Page | Route | APIs Required |
|------|-------|---------------|
| Dashboard | `/admin` | **Backend prerequisite:** admin summary APIs |
| Manage Field Types | `/admin/field-types` | `GET/POST/PUT/DELETE /api/v1/field-types` |
| Manage Users | `/admin/users` | **Backend prerequisite:** paginated user-list API; existing `PUT /api/v1/users/{id}/role` changes roles |
| Approve Fields | `/admin/fields` | **Backend prerequisite:** pending-field listing and approve/reject APIs |

---

## 8. Implementation Order (Recommended Phases)

### Phase 0: Backend and Contract Readiness
- [ ] Fix the gateway routes and public-route authorization listed in section 3.7
- [x] Add the owner-filtered field listing API
- [ ] Add the remaining admin APIs marked as backend prerequisites, or explicitly remove those pages from the release scope
- [ ] Publish OpenAPI documents in CI and generate the frontend API contracts
- [ ] Define environment-specific BFF and gateway origins, cookie settings, rate limits, and CORS policy

### Phase 1: Foundation
- [x] Initialize Next.js project with TypeScript
- [x] Set up the server-only gateway client and same-origin BFF endpoints
- [ ] Implement encrypted/signed cookie session handling and single-flight token refresh
- [x] Create Login page (OTP flow)
- [x] Set up optimistic redirects in `proxy.ts` and authoritative server-side checks in protected layouts/pages
- [x] Create app layout with Navbar
- [ ] Add error boundaries, loading states, structured logging, and request correlation IDs

### Phase 2: Field Browsing (Public)
- [x] Home page with field listing (paginated cards)
- [x] Field detail page with images, info, reviews
- [x] Operating hours display
- [ ] Field search/filter (**Backend prerequisite:** add supported search/filter query parameters)

### Phase 3: Booking (Client)
- [x] Availability checker component
- [x] Booking creation page/flow
- [x] My bookings list page
- [x] Booking detail with cancel option
- [x] Mock payment page

### Phase 4: Owner Features
- [x] Field creation form (with operating hours)
- [x] Field management dashboard
- [x] Sub-field CRUD
- [x] Image upload & management
- [x] Closure management
- [x] Owner's bookings view

### Phase 5: Admin Features
- [x] Field types CRUD
- [ ] User management with role changes
- [ ] Field approval workflow

### Phase 6: Notifications & Polish
- [x] Notification bell with unread badge
- [x] Notifications page
- [x] Mark read / mark all read
- [x] Profile page with edit
- [x] Error handling & loading states
- [x] Responsive design polish

---

## 9. Key Technical Considerations

1. **Authentication and sessions**:
   - Keep all tokens in the server/BFF boundary; browser components consume only a safe session/user DTO.
   - Use `proxy.ts` only for fast optimistic redirects and repeat authorization checks near protected data and mutations.
   - Clear cookies and cached user data when refresh fails. Never retry a request more than once after a 401.
   - The fixed `111111` OTP is an explicit development constraint, not a production security control.

2. **API Client Configuration**:
   - The gateway URL is a server-only environment variable and must not use the `NEXT_PUBLIC_` prefix.
   - Browser code calls relative same-origin BFF paths only.
   - Use native `fetch` with explicit timeouts, cache/revalidation behavior, correlation IDs, and typed error mapping.
   - Do not blindly unwrap `ApiResponse<T>`: non-2xx responses currently use `ErrorResponse`, so model and test both shapes.
   - Server Components should call the server-only gateway client directly rather than making an extra HTTP call through a local Route Handler.

3. **Query Key Structure** (TanStack Query, client-interactive data only):
   - `['fields']` - all fields
   - `['fields', id]` - single field
   - `['fields', id, 'operating-hours']` - operating hours
   - `['sub-fields', fieldId]` - sub-fields for field
   - `['reviews', fieldId]` - reviews
   - `['bookings', 'my', { page, size }]` - my bookings
   - `['bookings', 'owner', { page, size }]` - owner bookings
   - `['bookings', id]` - single booking
   - `['availability', subFieldId, date]` - availability
   - `['notifications']` - notifications
   - `['notifications', 'unread']` - unread notifications
   - `['notifications', 'unread-count']` - unread count

4. **Role-Based Access Control**:
   - Owner UI requires `userType === 'OWNER'`; admin UI requires `userType === 'ADMIN'`.
   - Enforce these checks in protected server layouts/pages and again at every backend operation.
   - Treat 401 as an expired/missing session and 403 as an authenticated user lacking permission.

5. **Form Validation**:
   - Phone number: regex `/^(0|\+84)[0-9]{9}$/`
   - OTP: 6 digits
   - Prices: Vietnamese Dong (VND) - integer amounts
   - Times: HH:mm:ss format
   - Validate on both the BFF and backend; client validation is for feedback only.

6. **Booking correctness**:
   - Availability shown in the UI is advisory; the booking create response is authoritative because another user may take the slot.
   - Prevent duplicate submissions and add an idempotency-key contract before enabling real payments.
   - Do not automatically retry booking/payment mutations.
   - Define venue timezone behavior explicitly (currently `Asia/Ho_Chi_Minh`) for dates and local times.

7. **Caching and invalidation**:
   - Public field pages may use server caching/revalidation; authenticated user, booking, and notification responses must be private/no-store.
   - Invalidate field, booking, availability, notification-count, and profile data after successful mutations.

8. **Uploads**:
   - Validate file count, MIME type, actual file signature, and size on the server.
   - Prefer signed direct-to-object-storage uploads for large files; never rely only on browser validation.
   - Restrict remote image hosts and transformations in Next.js image configuration.

---

## 10. Production Readiness Checklist

- [ ] Gateway routes, authorization rules, CORS, and WebSocket origins are verified in an integration environment
- [ ] Tokens are absent from browser storage, URLs, client logs, analytics, and browser-visible API responses
- [ ] Cookie flags, CSRF/origin checks, logout, refresh rotation, session revocation, and concurrent refresh are tested
- [ ] OTP send and verify endpoints have per-phone and per-IP rate limits; fixed OTP use is explicitly accepted for the target environment
- [ ] OpenAPI-generated contracts have no drift from backend controller tests
- [ ] Critical Playwright flows cover login, booking conflict, cancellation, owner management, and admin authorization
- [ ] Accessibility checks cover keyboard navigation, focus management, labels, contrast, and reduced motion
- [ ] CSP and other security headers, dependency scanning, secret scanning, and upload limits are enabled
- [ ] Structured logs, correlation IDs, metrics, tracing, alerting, and frontend error reporting are configured
- [ ] `next build`, lint, type checking, unit/integration tests, and E2E smoke tests are required in CI
- [ ] Backup/restore and rollback procedures are documented for the production release
