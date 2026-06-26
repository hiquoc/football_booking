# Football Field Booking System
[![Ask DeepWiki](https://devin.ai/assets/askdeepwiki.png)](https://deepwiki.com/hiquoc/football_booking)

This repository contains the backend microservices for a comprehensive Football Field Booking platform. The system allows field owners to manage their venues, sub-fields, and pricing, while clients can search for available slots and make bookings.

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

-   **Notification Service (`notification-service`):** A placeholder service designed to consume notification events from Kafka and send alerts, such as booking confirmations via email.

### Data Flow & Communication

-   **Synchronous:** Client -> Gateway -> Downstream Service (for user actions).
-   **Asynchronous (Event-Driven):** `Field Service` publishes data updates to Kafka. `Booking Service` consumes these events to update its internal database projections. This decouples the booking process from the field management process, enhancing performance and resilience.

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
    -   A mock payment endpoint transitions the booking to `CONFIRMED`.
    -   A background scheduler automatically moves untended `PENDING` bookings to `EXPIRED`.
    -   Bookings can be cancelled by the client or the field owner.
-   **Image & Review System:**
    -   Multi-file image uploads for fields managed via Cloudinary.
    -   Ability to re-order images and set a primary cover photo.
    -   Clients can submit ratings and comments for fields, which automatically updates the field's average rating.

## Technology Stack

-   **Backend:** Java 21, Spring Boot 3, Spring Cloud (Gateway, Eureka), Spring Data JPA, Spring Security, Spring Kafka
-   **Databases:** PostgreSQL (per-service), Redis (for caching, OTP, and session tracking)
-   **Messaging:** Apache Kafka & Zookeeper
-   **Image Storage:** Cloudinary
-   **Build & Dependencies:** Maven
-   **API Documentation:** OpenAPI 3 (Swagger UI)

## Getting Started

### Prerequisites

-   Java 21 SDK
-   Apache Maven
-   Docker and Docker Compose
-   Docker and Docker Compose can start local PostgreSQL, Kafka, and Redis for development.

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
*_DB_USERNAME=football
*_DB_PASSWORD=football
KAFKA_BOOTSTRAP_SERVERS=localhost:9092
REDIS_HOST=localhost
REDIS_PORT=6379
EUREKA_DEFAULT_ZONE=http://localhost:8761/eureka/
INTERNAL_GATEWAY_SECRET=dev-internal-gateway-secret
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
```

### 2. Run Infrastructure

Start the local PostgreSQL, Kafka, and Redis containers using Docker Compose.

```bash
cd backend/
docker-compose up -d
```

### 3. Run Microservices

Start each microservice in a separate terminal. It is recommended to start them in the following order to ensure dependencies are available.

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

6.  **Gateway Service:**
    ```bash
    cd backend/gateway-service/
    mvn spring-boot:run
    ```

The system is now running. The API Gateway is accessible at `http://localhost:8080`.

## API Documentation

Each service exposes its own OpenAPI (Swagger) documentation. After starting the services, you can access them at the following URLs:

-   **User Service:** `http://localhost:8081/swagger-ui.html`
-   **Field Service:** `http://localhost:8082/swagger-ui.html`
-   **Booking Service:** `http://localhost:8083/swagger-ui.html`

All API calls should be made through the Gateway at `http://localhost:8080`. The Swagger UIs for the individual services are useful for exploring the available endpoints.
