# Project Context

This repository is a full-stack football field booking platform. Use this file as the quick architecture map before changing code.

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

Booking service owns booking creation, availability checks, recurring bookings, cancellation, completion, and expiration.

Important booking concepts:

- Availability is generated from booking-service local projections plus existing bookings.
- Active/reserving bookings block overlapping slots.
- Pricing is based on time price rules and can span multiple price periods.
- Pending bookings expire through a scheduler.
- Confirmed bookings complete through a scheduler after their end time.
- Booking repository methods intentionally use bulk `@Modifying` updates for status transitions.

The active file from the IDE, `backend/booking-service/src/main/java/com/project/booking/repository/BookingRepository.java`, is part of this booking persistence boundary.

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
- `payment.success`
- `payment.failed`
- `user.balance.refund-requested`
- `user.balance.deduction-requested`

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
