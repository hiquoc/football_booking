# Logging Architecture

The backend uses SLF4J with Logback in every runnable service. Development profiles (`dev`, `local`, `test`) write readable console logs. Production and other profiles write JSON logs through `logstash-logback-encoder`.

Each log includes an ISO-8601 timestamp, level, thread, logger, message, stack trace when present, `service`, and MDC fields.

## Correlation ID Flow

- Gateway reads `X-Request-ID`.
- If missing, gateway generates a UUID.
- Gateway forwards `X-Request-ID` to downstream services.
- Servlet services read `X-Request-ID`, put it in MDC as `requestId`, return it on the response, and clear MDC after the request.
- Outbox-created Kafka messages copy MDC `requestId` into Kafka headers so consumers can continue the same trace.

## MDC Fields

- `requestId`: request correlation ID from `X-Request-ID`.
- `serviceName`: service currently writing the log.
- `userId`: authenticated user when available, otherwise omitted or `anonymous`.
- `bookingId`: booking aggregate when code explicitly places it in MDC.
- `fieldId`: field aggregate when code explicitly places it in MDC.
- `paymentId`: payment aggregate when code explicitly places it in MDC.

## Log Levels

- `TRACE`: very detailed local diagnostics.
- `DEBUG`: cache and Redis lock attempts, non-production troubleshooting.
- `INFO`: request completion, business events, Kafka send/receive/success.
- `WARN`: client/business rejections, booking conflicts, retry scheduling, lock timeouts, webhook verification failures.
- `ERROR`: unhandled exceptions, Kafka publish/process failures, DLQ publication, unexpected Redis failures.

## HTTP Logging

Request filters log `request_started` and `request_completed` without request bodies, headers, query strings, JWTs, or credentials.

Example development log:

```text
2026-07-29T12:00:00.123Z INFO  [http-nio-8081-exec-1] c.p.common.security.IncomingRequestLogFilter service=booking-service requestId=3a7... userId=... - request_completed service=booking-service method=POST path=/api/bookings status=201 durationMs=42 userId=...
```

Example production log:

```json
{"timestamp":"2026-07-29T12:00:00.123Z","level":"INFO","thread":"http-nio-8081-exec-1","logger":"com.project.common.security.IncomingRequestLogFilter","service":"booking-service","requestId":"3a7...","userId":"...","message":"request_completed service=booking-service method=POST path=/api/bookings status=201 durationMs=42 userId=..."}
```

## Kafka Logging

Producer logs:

- `kafka_producer_send_started`
- `kafka_producer_send_succeeded`
- `kafka_producer_send_failed`
- `kafka_producer_retry_scheduled`
- `kafka_dlq_published`

Consumer logs:

- `kafka_consumer_received`
- `kafka_consumer_processed`
- `kafka_consumer_processing_failed`
- `kafka_consumer_retry_scheduled`
- `kafka_consumer_dlq_required`

Kafka headers:

- `eventId`
- `eventType`
- `aggregateId`
- `requestId`

## Sensitive Data

Never log passwords, JWTs, refresh tokens, OTP codes, Stripe secrets, card data, or database credentials. Log stable IDs and masked values only, such as a phone suffix.

Use parameterized logging:

```java
log.info("booking_created eventType=booking_created bookingId={} userId={}", bookingId, userId);
```

Do not use string concatenation:

```java
log.info("User " + userId);
```

## Loki Queries

Find all logs for one request:

```logql
{service="booking-service"} | json | requestId="3a7..."
```

Find failed Kafka publishes:

```logql
{service=~".+"} | json | message =~ ".*kafka_producer_send_failed.*"
```

Find slow HTTP requests:

```logql
{service=~".+"} | json | message =~ ".*request_completed.*" | durationMs > 1000
```

Find unhandled exceptions:

```logql
{service=~".+"} | json | message =~ ".*unhandled_request_error.*"
```

Find payment events:

```logql
{service="payment-service"} | json | message =~ ".*payment_.*"
```
