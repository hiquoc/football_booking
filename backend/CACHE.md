# Redis Cache Strategy

The services use Spring Cache backed by Redis through the shared `common` cache configuration. Cache keys and cache names live in `com.project.common.cache` so services use the same naming scheme.

Cached reads:
- `user-by-id`: `user:{userId}` for user profile, avatar, role, and balance DTOs.
- `field-detail`: `field:{fieldId}:viewer:{viewer}` for field detail responses.
- `field-search`: `field-search:{sha256(allSearchParametersAndViewer)}` for field card search.
- `availability`: `availability:{subFieldId}:{date}` for generated availability responses.
- `lookup-field-types`: `lookup:field-types` for field type lookup data.

Invalidation is explicit and does not rely on TTL:
- User profile, avatar, role, and balance writes evict `user-by-id`.
- Field, image, schedule, sub-field, and favorite mutations clear field detail/search caches.
- Booking create/cancel/payment-confirm operations evict affected availability keys.
- Booking expiry/completion and field projection Kafka events clear availability caches.

TTLs are safety bounds only. `@Cacheable(sync = true)` is used on cacheable reads to reduce cache stampede within each service instance when a key is missing.
