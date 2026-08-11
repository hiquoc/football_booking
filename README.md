# Football Field Booking System
[![Ask DeepWiki](https://devin.ai/assets/askdeepwiki.png)](https://deepwiki.com/hiquoc/football_booking)

This repository contains a full-stack Football Field Booking platform. The system allows field owners to manage venues, sub-fields, schedules, pricing, images, and bookings, while clients can search available slots, book fields, pay online, and receive real-time notifications.

## Architecture

The system is built on a microservices architecture, promoting separation of concerns, scalability, and independent deployment. The core services communicate asynchronously via Apache Kafka for an event-driven approach, ensuring loose coupling and resilience.

### Services

-   **Gateway Service (`gateway-service`):** The single entry point for all client requests. It handles routing to downstream services, authenticates users by validating JWTs, and injects user identity headers (`X-User-Id`, `X-User-Role`) for use by other services.

-   **Discovery Service (`discovery-service`):** A Netflix Eureka server that provides service registration and discovery, allowing services to locate each other dynamically.

-   **User Service (`user-service`):** Manages all aspects of user accounts and authentication.
    -   Handles OTP-based login/registration via phone number.
    -   Supports social login with Google and Facebook (OAuth2).
    -   Issues and refreshes JWT tokens.
    -   Manages user profiles and role assignments (ADMIN, OWNER, CLIENT).

-   **Field Service (`field-service`):** The source of truth for all field-related information.
    -   CRUD operations for fields (venues), sub-fields (e.g., Pitch A, Pitch B), and sport types (e.g., Football, Badminton).
    -   Manages complex schedules, including weekly operating hours and temporary closures.
    -   Handles image uploads to Cloudinary.
    -   Manages user reviews and calculates average ratings.
    -   Publishes all data changes as events to Kafka topics (e.g., `field.sub-field.updated.v1`).

-   **Booking Service (`booking-service`):** Responsible for the core booking and availability logic.
    -   Consumes events from the Field Service to maintain a local, read-optimized projection of field data (schedules, pricing, etc.).
    -   Provides high-performance availability checks without calling the Field Service directly.
    -   Handles booking creation, cancellation, and status changes.
    -   Implements pricing logic based on time-based rules.
    -   Includes a scheduler to automatically expire unpaid, `PENDING` bookings.

-   **Payment Service (`payment-service`):** Owns payment sessions, provider integration, and booking payment projections.
    -   Creates Stripe checkout sessions for card payments.
    -   Handles provider webhook events idempotently.
    -   Publishes payment result events consumed by the Booking Service.

-   **Notification Service (`notification-service`):** Consumes notification events and delivers user-facing alerts.
    -   Sends email notifications using HTML templates.
    -   Delivers in-app notifications in real time over authenticated STOMP/WebSocket connections.
    -   The browser obtains a 60-second WebSocket ticket through the Next.js BFF; access and refresh tokens remain HttpOnly.
    -   WebSocket handshakes pass through the API gateway, which validates the ticket and relays trusted user headers to the notification service.

-   **Frontend (`frontend`):** A Next.js App Router application with a BFF layer.
    -   Provides client, owner, and admin experiences.
    -   Keeps access and refresh tokens in HttpOnly cookies.
    -   Proxies browser requests through server-side route handlers to the API Gateway.

### Data Flow & Communication

-   **Synchronous:** Browser -> Next.js BFF -> Gateway -> Downstream Service.
-   **Asynchronous (Event-Driven):** Field, booking, payment, and notification workflows communicate through Kafka topics. The Booking Service keeps local projections of field data, and payment/notification events are processed with inbox/outbox-style handlers for idempotency and resilience.

## Key Features

-   **User Management:**
    -   OTP login with Redis-based cooldowns and attempt limits.
    -   Social login integration with Google & Facebook.
    -   JWT-based authentication and authorization with refresh tokens.
    -   Role-based access control (`CLIENT`, `OWNER`, `ADMIN`).
-   **Field & Sub-Field Management:**
    -   Owners can register and manage their sports venues.
    -   Define granular sub-fields with specific types (e.g., `FOOTBALL_5V5`), amenities, and surfaces.
    -   Set complex, time-based pricing rules for each sub-field (e.g., peak vs. off-peak hours).
    -   Configure default booking rules (min/max duration).
-   **Scheduling & Availability:**
    -   Define weekly operating hours for an entire field or override them for specific sub-fields.
    -   Schedule temporary closures for maintenance.
    -   Real-time availability checks for any given date.
-   **Booking Workflow:**
    -   Bookings start in a `PENDING` state.
    -   Payments can be completed through Stripe checkout or user balance.
    -   Payment events transition bookings to confirmed, failed, expired, or cancelled states.
    -   A background scheduler automatically moves untended `PENDING` bookings to `EXPIRED`.
    -   Bookings can be cancelled by the client or the field owner.
    -   A booking spanning multiple price periods is charged proportionally for each overlapping period; the final VND amount is rounded up to the nearest `1,000`.
-   **Payments & Notifications:**
    -   Stripe checkout session creation and webhook handling.
    -   User balance deduction and refund event flow.
    -   In-app, WebSocket, and email notification delivery.
-   **Image & Review System:**
    -   Multi-file image uploads for fields managed via Cloudinary.
    -   Ability to re-order images and set a primary cover photo.
    -   Clients can submit ratings and comments for fields, which automatically updates the field's average rating.
-   **Frontend Application:**
    -   Search and field detail pages.
    -   Booking and payment screens.
    -   Owner field, schedule, image, closure, and booking management.
    -   Admin field, field type, and user management.

## Technology Stack

-   **Frontend:** Next.js 16, React 19, TypeScript, TanStack Query, Tailwind CSS
-   **Backend:** Java 21, Spring Boot 3, Spring Cloud (Gateway, Eureka), Spring Data JPA, Spring Security, Spring Kafka
-   **Databases:** PostgreSQL (per-service), Redis (for caching, OTP, and session tracking)
-   **Messaging:** Apache Kafka
-   **Payments:** Stripe Checkout and webhooks
-   **Image Storage:** Cloudinary
-   **Build & Dependencies:** Maven, npm
-   **Containerization:** Docker and Docker Compose
-   **API Documentation:** OpenAPI 3 (Swagger UI)

## Getting Started

### Prerequisites

-   Java 21 SDK
-   Apache Maven
-   Node.js and npm
-   Docker and Docker Compose
-   Docker and Docker Compose can start the full local stack for development.

### 1. Environment Configuration

The backend uses Spring profiles selected by `APP_MODE`.

- `APP_MODE=DEV` or no `APP_MODE`: loads `application-dev.yaml`, optionally imports `backend/common/.env-dev.properties`, uses local Docker defaults, and bypasses the downstream internal gateway secret check.
- `APP_MODE=PROD`: loads `application-prod.yaml`, optionally imports `backend/common/.env-prod.properties`, requires real secrets and database settings, and rejects downstream requests without the internal gateway secret.

For local development, you can start with no env file. The default local values are:

```properties
USER_DB_URL=jdbc:postgresql://localhost:5432/user_db
FIELD_DB_URL=jdbc:postgresql://localhost:5432/field_db
BOOKING_DB_URL=jdbc:postgresql://localhost:5432/booking_db
NOTIFICATION_DB_URL=jdbc:postgresql://localhost:5432/notification_db
PAYMENT_DB_URL=jdbc:postgresql://localhost:5432/payment_db
*_DB_USERNAME=football
*_DB_PASSWORD=football
KAFKA_BOOTSTRAP_SERVERS=localhost:9092
REDIS_HOST=localhost
REDIS_PORT=6379
EUREKA_DEFAULT_ZONE=http://localhost:8761/eureka/
INTERNAL_GATEWAY_SECRET=dev-internal-gateway-secret
API_BASE_URL=http://localhost:8080
NEXT_PUBLIC_APP_URL=http://localhost:3000
NEXT_PUBLIC_GATEWAY_WS_URL=ws://localhost:8080/ws
```

For production, create `backend/common/.env-prod.properties` and provide all required values. Use the same `INTERNAL_GATEWAY_SECRET` in the gateway and every downstream service:

```properties
# Database URLs and Credentials
USER_DB_URL=jdbc:postgresql://localhost:5432/user_db
USER_DB_USERNAME=your_username
USER_DB_PASSWORD=your_password

FIELD_DB_URL=jdbc:postgresql://localhost:5432/field_db
FIELD_DB_USERNAME=your_username
FIELD_DB_PASSWORD=your_password

BOOKING_DB_URL=jdbc:postgresql://localhost:5432/booking_db
BOOKING_DB_USERNAME=your_username
BOOKING_DB_PASSWORD=your_password

NOTIFICATION_DB_URL=jdbc:postgresql://localhost:5432/notification_db
NOTIFICATION_DB_USERNAME=your_username
NOTIFICATION_DB_PASSWORD=your_password

PAYMENT_DB_URL=jdbc:postgresql://localhost:5432/payment_db
PAYMENT_DB_USERNAME=your_username
PAYMENT_DB_PASSWORD=your_password

# JWT Configuration
JWT_SECRET=your_super_strong_base64_encoded_jwt_secret_key
JWT_EXPIRATION=3600000 # 1 hour in ms
JWT_REFRESH_EXPIRATION=604800000 # 7 days in ms

# Gateway-to-service internal protection
INTERNAL_GATEWAY_SECRET=your_shared_gateway_secret

# Cloudinary API Credentials
CLOUDINARY_CLOUD_NAME=your_cloudinary_cloud_name
CLOUDINARY_API_KEY=your_cloudinary_api_key
CLOUDINARY_API_SECRET=your_cloudinary_api_secret

# OAuth2 Credentials (Optional)
GOOGLE_CLIENT_ID=your_google_client_id
GOOGLE_CLIENT_SECRET=your_google_client_secret
FACEBOOK_CLIENT_ID=your_facebook_client_id
FACEBOOK_CLIENT_SECRET=your_facebook_client_secret

# SMTP Credentials for Notification Service (Optional)
SMTP_HOST=smtp.gmail.com
SMTP_PORT=587
SMTP_USERNAME=your_email@gmail.com
SMTP_PASSWORD=your_app_password

# Stripe
STRIPE_SECRET_KEY=your_stripe_secret_key
STRIPE_WEBHOOK_SECRET=your_stripe_webhook_secret

# Frontend/BFF
API_BASE_URL=http://localhost:8080
NEXT_PUBLIC_APP_URL=http://localhost:3000
NEXT_PUBLIC_GATEWAY_WS_URL=wss://your-domain.example/ws
```

### 2. Run With Docker Compose

Start the full local stack using Docker Compose. The local file binds browser-facing services to localhost ports:
frontend `8000`, gateway `8080`, services `8081`-`8085`, discovery `8071`, and Mailpit `8025`.

```bash
cd backend/
docker compose -f docker-compose.local.yml up -d
```

The frontend is available at `http://localhost:8000`, and the API Gateway is available at `http://localhost:8080`.

For production, provide the required production environment variables and run:

```bash
cd backend/
docker compose -f docker-compose.prd.yml up -d --build
```

### 3. Run Services Manually

If you prefer running services outside Docker, start PostgreSQL, Kafka, Redis, and Mailpit first, then run each service in a separate terminal. It is recommended to start them in the following order to ensure dependencies are available.

1.  **Discovery Service:**
    ```bash
    cd backend/discovery-service/
    mvn spring-boot:run
    ```

2.  **User Service:**
    ```bash
    cd backend/user-service/
    mvn spring-boot:run
    ```

3.  **Field Service:**
    ```bash
    cd backend/field-service/
    mvn spring-boot:run
    ```

4.  **Booking Service:**
    ```bash
    cd backend/booking-service/
    mvn spring-boot:run
    ```
5.  **Notification Service:**
    ```bash
    cd backend/notification-service/
    mvn spring-boot:run
    ```

6.  **Payment Service:**
    ```bash
    cd backend/payment-service/
    mvn spring-boot:run
    ```

7.  **Gateway Service:**
    ```bash
    cd backend/gateway-service/
    mvn spring-boot:run
    ```

8.  **Frontend:**
    ```bash
    cd frontend/
    npm install
    npm run dev
    ```

The system is now running. The frontend is accessible at `http://localhost:3000`, and the API Gateway is accessible at `http://localhost:8080`.

### Development demo data

With the default `dev` profile, Flyway automatically loads a complete linked data set across all service databases. It includes users, venues, sub-fields, schedules, prices, images, reviews, closures, bookings, and notifications. Production only runs schema migrations and never loads demo rows.

Use the development OTP `111111` with one of these phone numbers:

| Role | Phone numbers |
|------|---------------|
| Admin | `0900000001` |
| Owner | `0900000011`, `0900000012`, `0900000013` |
| Client | `0900000021` through `0900000025` |

## API Documentation

Each service exposes its own OpenAPI (Swagger) documentation. After starting the services, you can access them at the following URLs:

-   **User Service:** `http://localhost:8081/swagger-ui.html`
-   **Field Service:** `http://localhost:8082/swagger-ui.html`
-   **Booking Service:** `http://localhost:8083/swagger-ui.html`
-   **Notification Service:** `http://localhost:8084/swagger-ui.html`
-   **Payment Service:** `http://localhost:8085/swagger-ui.html`

All API calls should be made through the Gateway at `http://localhost:8080`. The Swagger UIs for the individual services are useful for exploring the available endpoints.

## Verification

Before committing, run:

```bash
cd frontend
npm run lint
npm run typecheck
npm test
npm run build

cd ../backend
./mvnw test
docker compose config
```

## Load Testing

A k6 mock load test is available in `load-tests/`. Start with:

```bash
TARGET_URL=http://YOUR_VPS_IP:8080 TEST_PROFILE=vps k6 run load-tests/server-load-smoke.js
```
