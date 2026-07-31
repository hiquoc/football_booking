# Project Context

This repository is a full-stack football field booking platform. Use this file as the quick architecture map before changing code.

Last updated: 2026-07-29

## Current Progress

The project is in active feature-integration work, with the main product surfaces already implemented across backend microservices and the Next.js BFF/frontend.

Completed or largely implemented:

- Microservice foundation: gateway, Eureka discovery, shared common module, per-service PostgreSQL databases, Redis, Kafka, and Docker Compose local stack.
- User/auth: OTP login, OAuth login support, JWT/refresh-token flow, role-based access for `ADMIN`, `OWNER`, `EMPLOYEE`, and `CLIENT`, user profiles, avatar upload, wallet/balance support, and public profile lookup.
- Field management: owner CRUD for fields and sub-fields, field types, schedules, closures, pricing rules, Cloudinary image workflow, favorites, reviews, employee assignment, and field-management permission checks.
- Booking: availability calculation from booking-service projections, booking creation/cancellation, owner booking lists, client booking history, recurring bookings, match results, no-show reports, payment-related state transitions, pending expiration, and completed-booking scheduler flow.
- Payments: Stripe checkout/webhooks, payment sessions, user-balance payment strategy, wallet top-up events, and payment result events consumed by booking/notification flows.
- Notifications: email/in-app notification creation, STOMP/WebSocket delivery, authenticated WebSocket ticket flow through the BFF and gateway, and inbox/outbox-style processing.
- Community: looking-for-opponent/player posts, post applications, owner decisions, post reporting, owner hide flow, moderation history, player statistics, and dedicated "my posts" / "my applications" frontend routes.
- Frontend: public field search/detail, booking and payment pages, owner field/booking/employee tools, admin field/user/moderation tools, community feed/detail/create pages, notifications UI, and BFF route handlers.

Current uncommitted work in the tree is concentrated around:

- Community feed filtering and navigation, including `ownerId`, `applicantId`, status `all`, default upcoming sorting, location filters, field-name suggestions, and new routes under `frontend/src/app/(main)/community/my-posts` and `frontend/src/app/(main)/community/my-applications`.
- Client booking history filters by booking date and status, wired from frontend query keys/hooks/BFF through `BookingController.getMyBookings`.
- Owner/employee booking moderation, where assigned `EMPLOYEE` users can report no-shows, view violations, view banned clients, and unban through field-management permission checks.
- Field employee assignment notifications via the new `field.employee.assigned` notification topic and `FieldEmployeeAssignedEvent`.
- Wallet top-up success notification handling in notification-service.

Known validation status:

- Some unit tests were updated in booking and notification services.
- Full verification should still be run before commit: backend tests, frontend lint/typecheck/tests/build, and Docker Compose config validation.

## Repository Layout

```text
.
+-- backend/                 # Java 21 Spring Boot microservices
|   +-- common/              # Shared DTOs, events, exceptions, cache, security, inbox/outbox
|   +-- discovery-service/   # Eureka service registry
|   +-- gateway-service/     # Spring Cloud Gateway entry point
|   +-- user-service/        # Auth, users, profiles, roles, balances, avatar upload
|   +-- field-service/       # Fields, sub-fields, schedules, pricing, images, reviews
|   +-- booking-service/     # Availability, bookings, recurring bookings, booking projections
|   +-- payment-service/     # Stripe checkout, webhooks, payment sessions/projections
|   +-- notification-service/# Email, in-app notifications, STOMP/WebSocket
+-- frontend/                # Next.js App Router frontend with BFF route handlers
```

## Runtime Architecture

The browser does not call backend services directly.

```text
Browser
  -> Next.js BFF route handlers under frontend/src/app/api/**
  -> API Gateway on :8080
  -> Spring services discovered through Eureka
```

Asynchronous workflows use Kafka. Services use local databases and communicate through events where practical instead of direct cross-service writes.

Local infrastructure from `backend/docker-compose.yml`:

- PostgreSQL 16 with separate databases: `user_db`, `field_db`, `booking_db`, `notification_db`, `payment_db`
- Kafka
- Redis
- Mailpit
- Eureka discovery service
- Gateway and all backend services
- Next.js frontend
- Optional Stripe CLI profile

## Service Ports

- Frontend: `3000`
- Gateway: `8080`
- User Service: `8081`
- Field Service: `8082`
- Booking Service: `8083`
- Notification Service: `8084`
- Payment Service: `8085`
- Discovery Service: `8761`
- PostgreSQL: `5432`
- Redis: `6379`
- Mailpit UI: `8025`

## Gateway Routes

Gateway routing is defined in `backend/gateway-service/src/main/resources/application.yaml`.

- `/api/v1/auth/**` -> user-service
- `/api/v1/users/**` -> user-service
- `/api/chat` -> user-service
- `/api/v1/users/me/favorites/**` -> field-service
- `/api/v1/fields/**`, `/api/v1/sub-fields/**`, `/api/v1/field-types/**`, `/api/v1/reviews/**` -> field-service
- `/api/v1/bookings/**`, `/api/v1/admin/booking-config` -> booking-service
- `/api/v1/owner/**` booking moderation routes -> booking-service
- `/api/v1/notifications/**` -> notification-service
- `/api/v1/payments/**` -> payment-service
- `/ws/**` -> notification-service WebSocket

The gateway validates JWTs and injects trusted identity headers such as `X-User-Id` and `X-User-Role` for downstream services. In production, downstream services also require the shared internal gateway secret.

## Backend Conventions

Backend is a Maven multi-module project under `backend/pom.xml`.

- Java 21
- Spring Boot 3.5.x
- Spring Cloud 2025.x
- Spring Data JPA
- Spring Security
- Spring Kafka
- Flyway migrations
- MapStruct
- Lombok
- springdoc OpenAPI

Common cross-service code belongs in `backend/common`, including:

- API envelopes and pagination DTOs
- Shared exceptions and global handlers
- Authentication principal/filter helpers
- Shared enum types such as booking status
- Kafka event contracts
- Inbox/outbox support
- Redis cache naming/configuration

When changing backend APIs, update Swagger/OpenAPI annotations, DTO annotations, validation examples, and tests. This is mandatory per `backend/AGENTS.md`.

## Frontend Conventions

Frontend is a Next.js 16, React 19, TypeScript app.

Important separation rules from `frontend/CODE_STRUCTURE_RULES.md`:

- Pages in `src/app/**/page.tsx` should compose components and prefetch data only.
- UI components live in `src/components/**` and should not call `fetch` directly.
- Browser code calls same-origin `/api/**` only.
- BFF route handlers live in `src/app/api/**/route.ts`.
- Server-only gateway access lives in `src/lib/server/**` and should import `server-only`.
- Client request helpers live in `src/lib/client/**`.
- React Query hooks live in `src/lib/hooks/**`.
- Query keys are centralized in `src/lib/query-keys.ts`.
- Shared schemas/types live in `src/lib/api/**`.
- User-facing UI text should be Vietnamese.

Typical frontend flow:

```text
Server Component
  -> server prefetch helper in src/lib/server/**
  -> API Gateway
  -> dehydrate React Query cache
  -> Client Component
  -> custom hook in src/lib/hooks/**
  -> client API in src/lib/client/**
  -> BFF route in src/app/api/**
```

## Core Domain Flow

### Field Data

Field service is the source of truth for venues, sub-fields, field types, operating hours, closures, pricing rules, images, favorites, and reviews.

Field mutations publish Kafka events. Booking service consumes those events to maintain local read projections for availability and booking decisions.

Field event topic constants are in:

`backend/common/src/main/java/com/project/common/events/field/FieldEventTopics.java`

Current field topics:

- `field.sub-field.created.v1`
- `field.sub-field.updated.v1`
- `field.sub-field.deleted.v1`
- `field.operating-hours.updated.v1`
- `field.sub-field-operating-hours.updated.v1`
- `field.closure.created.v1`
- `field.closure.updated.v1`
- `field.closure.deleted.v1`

### Booking

Booking service owns booking creation, availability checks, recurring bookings, cancellation, completion, expiration, match results, and booking moderation.

Important booking concepts:

- Availability is generated from booking-service local projections plus existing bookings.
- Active/reserving bookings block overlapping slots.
- Pricing is based on time price rules and can span multiple price periods.
- Pending bookings expire through a scheduler.
- Confirmed bookings complete through a scheduler after their end time.
- Client booking history supports filtering by `bookingDate` and `status`.
- Owner booking lists support filters by date, sub-field, and status.
- No-show reporting is allowed for completed bookings and is available to field owners and assigned employees.
- Field violation and banned-client actions must prove field-management access before mutating moderation state.
- Booking repository methods intentionally use bulk `@Modifying` updates for status transitions.

The active file from the IDE, `backend/booking-service/src/main/java/com/project/booking/repository/BookingRepository.java`, is part of this booking persistence boundary.

### Community

Community posts are owned by booking-service and are tied to confirmed booking context.

Important community concepts:

- Posts support `LOOKING_OPPONENT` and `LOOKING_PLAYER` workflows.
- Filters include post type, status, skill level, date, field type, city, district, field name, keyword, owner, applicant, and sort mode.
- The main feed defaults toward upcoming open posts.
- Logged-in clients and employees can see their own posts and applications through dedicated frontend routes.
- Post applications can be accepted/rejected by post owners.
- Reports, owner hide actions, moderation history, and player statistics are part of the same bounded context.

### Payment

Payment service owns payment sessions, Stripe checkout, provider webhooks, and payment status. Booking service reacts to payment result events instead of owning provider-specific payment state.

Booking supports at least:

- Stripe payment strategy
- User balance payment strategy

### Notification

Notification service consumes notification events and creates user-facing notifications through:

- Email templates
- In-app notification records
- STOMP/WebSocket delivery

Notification topics are defined in:

`backend/common/src/main/java/com/project/common/events/notification/NotificationEventTopics.java`

Current notification/payment/user-balance topics:

- `user.request-otp`
- `booking.created`
- `booking.confirmed`
- `booking.cancelled`
- `booking.completed`
- `payment.success`
- `payment.failed`
- `user.completed-booking-count.changed`
- `user.balance.top-up-succeeded`
- `user.balance.refund-requested`
- `user.balance.deduction-requested`
- `user.balance.updated`
- `user.profile.updated`
- `community.notification`
- `field.employee.assigned`
- `match.evaluation.submitted`
- `player.match-statistics.adjusted`
- `platform-ban.requested`
- `moderation.notification`

### Field Employees

Field owners can assign employees to help manage fields.

Important employee-management concepts:

- Field service owns employee assignments and validates candidate users through user-service.
- Assignment creates a field-domain record and publishes `FieldEmployeeAssignedEvent` to the notification topic `field.employee.assigned`.
- Notification service creates an in-app notification for the assigned employee.
- Booking moderation checks whether an `EMPLOYEE` can manage a field through the field-management client before allowing no-show, violation, banned-client, or unban actions.

## Cache Strategy

Redis-backed Spring Cache is configured through common code. See `backend/CACHE.md`.

Key cache areas:

- `user-by-id`
- `field-detail`
- `field-search`
- `availability`
- `lookup-field-types`

Invalidation is explicit and tied to writes or consumed events. TTLs are only safety bounds.

## Environment Profiles

Backend uses `APP_MODE`.

- `APP_MODE=DEV` or omitted: loads dev profile, uses local defaults, and bypasses downstream internal gateway secret checks.
- `APP_MODE=PROD`: loads prod profile, requires real secrets, database settings, and internal gateway secret enforcement.

Local defaults are suitable for Docker Compose development. Production secrets should go in `backend/common/.env-prod.properties`.

## Useful Commands

Start the full stack:

```bash
cd backend
docker compose up -d
```

Run all backend tests:

```bash
cd backend
./mvnw test
```

Run frontend checks:

```bash
cd frontend
npm run lint
npm run typecheck
npm test
npm run build
```

Validate Docker Compose:

```bash
cd backend
docker compose config
```

## Development Data

The dev profile loads demo users and linked domain data through Flyway dev migrations.

Demo OTP: `111111`

- Admin: `0900000001`
- Owners: `0900000011`, `0900000012`, `0900000013`
- Clients: `0900000021` through `0900000025`

## Change Checklist

Before making changes:

- Identify whether the change is frontend, backend, cross-service, or event-contract work.
- For frontend changes, read `frontend/AGENTS.md` and `frontend/CODE_STRUCTURE_RULES.md`.
- For backend API changes, update OpenAPI annotations, DTO validation annotations/examples, and tests.
- For event changes, update shared event classes/topics in `backend/common` and all producers/consumers.
- For database changes, add Flyway migrations in the owning service only.
- For cache-sensitive writes, check whether explicit cache invalidation is required.
- For booking availability, payment, or cancellation changes, check both synchronous service logic and async Kafka handlers.

## Recommended Next Steps

1. Review the current uncommitted feature set for consistency before adding more scope. Pay special attention to role naming (`OWNER`/`EMPLOYEE`), field-management authorization, and whether every frontend BFF route maps to a gateway route.
2. Run focused backend tests first:

```bash
cd backend
./mvnw -pl booking-service test
./mvnw -pl field-service test
./mvnw -pl notification-service test
```

3. Run frontend checks:

```bash
cd frontend
npm run lint
npm run typecheck
npm test
```

4. Start the stack and manually test these flows:

- Login as owner, assign an employee, confirm the employee receives an in-app notification.
- Login as employee, open owner booking tools, report a completed booking as no-show, then verify violation/banned-client views.
- Login as client, filter `/bookings` by date/status.
- Create community posts, apply to posts, verify `/community/my-posts` and `/community/my-applications`, and test status `all` filtering.

5. After verification, fix any failing tests or UX issues, then update API docs/Swagger annotations for any changed backend endpoints before committing.
