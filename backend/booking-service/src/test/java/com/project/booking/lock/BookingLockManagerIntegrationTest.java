package com.project.booking.lock;

import com.project.booking.exception.BookingInProgressException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers(disabledWithoutDocker = true)
class BookingLockManagerIntegrationTest {

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    private LettuceConnectionFactory connectionFactory;

    @AfterEach
    void tearDown() {
        if (connectionFactory != null) {
            connectionFactory.destroy();
        }
    }

    @Test
    void concurrentBookingsForSameSubFieldAndSlotAllowOnlyOneCriticalSection() throws Exception {
        BookingLockManager lockManager = lockManager(1, 1);
        UUID subFieldId = UUID.randomUUID();
        LocalDate date = LocalDate.now().plusDays(1);

        List<Result> results = runConcurrently(50, index -> lockManager.executeWithLock(
                subFieldId,
                LocalDateTime.of(date, LocalTime.of(8, 0)),
                LocalDateTime.of(date, LocalTime.of(9, 0)),
                () -> {
                    sleep(150);
                    return Result.SUCCESS;
                }));

        assertEquals(1, results.stream().filter(result -> result == Result.SUCCESS).count());
        assertEquals(49, results.stream().filter(result -> result == Result.BOOKING_IN_PROGRESS).count());
    }

    @Test
    void bookingsForDifferentSubFieldsDoNotBlockEachOther() throws Exception {
        BookingLockManager lockManager = lockManager(1, 1);
        LocalDate date = LocalDate.now().plusDays(1);
        AtomicInteger activeCriticalSections = new AtomicInteger();
        AtomicInteger maxActiveCriticalSections = new AtomicInteger();

        List<Result> results = runConcurrently(20, index -> lockManager.executeWithLock(
                UUID.randomUUID(),
                LocalDateTime.of(date, LocalTime.of(8, 0)),
                LocalDateTime.of(date, LocalTime.of(9, 0)),
                () -> {
                    int active = activeCriticalSections.incrementAndGet();
                    maxActiveCriticalSections.accumulateAndGet(active, Math::max);
                    try {
                        sleep(75);
                        return Result.SUCCESS;
                    } finally {
                        activeCriticalSections.decrementAndGet();
                    }
                }));

        assertEquals(20, results.stream().filter(result -> result == Result.SUCCESS).count());
        assertTrue(maxActiveCriticalSections.get() > 1);
    }

    @Test
    void crossDateBookingLocksBothBookingDates() throws Exception {
        BookingLockManager lockManager = lockManager(1, 1);
        UUID subFieldId = UUID.randomUUID();
        LocalDate date = LocalDate.now().plusDays(1);

        CountDownLatch firstLockHeld = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Result> overnight = executor.submit(() -> lockManager.executeWithLock(
                    subFieldId,
                    LocalDateTime.of(date, LocalTime.of(23, 0)),
                    LocalDateTime.of(date.plusDays(1), LocalTime.of(1, 0)),
                    () -> {
                        firstLockHeld.countDown();
                        sleep(150);
                        return Result.SUCCESS;
                    }));
            assertTrue(firstLockHeld.await(1, TimeUnit.SECONDS));

            Future<Result> nextDay = executor.submit(() -> {
                try {
                    return lockManager.executeWithLock(
                            subFieldId,
                            LocalDateTime.of(date.plusDays(1), LocalTime.MIDNIGHT),
                            LocalDateTime.of(date.plusDays(1), LocalTime.of(1, 0)),
                            () -> Result.SUCCESS);
                } catch (BookingInProgressException ex) {
                    return Result.BOOKING_IN_PROGRESS;
                }
            });

            assertEquals(Result.SUCCESS, overnight.get(2, TimeUnit.SECONDS));
            assertEquals(Result.BOOKING_IN_PROGRESS, nextDay.get(2, TimeUnit.SECONDS));
        } finally {
            executor.shutdownNow();
        }
    }

    private BookingLockManager lockManager(int maxAttempts, long initialBackoffMs) {
        connectionFactory = new LettuceConnectionFactory(redis.getHost(), redis.getMappedPort(6379));
        connectionFactory.afterPropertiesSet();
        StringRedisTemplate redisTemplate = new StringRedisTemplate(connectionFactory);
        redisTemplate.afterPropertiesSet();
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushDb();

        BookingLockManager lockManager = new BookingLockManager(new RedisDistributedLockService(redisTemplate));
        ReflectionTestUtils.setField(lockManager, "maxAttempts", maxAttempts);
        ReflectionTestUtils.setField(lockManager, "initialBackoffMs", initialBackoffMs);
        ReflectionTestUtils.setField(lockManager, "lockTtlMs", 10_000L);
        return lockManager;
    }

    private List<Result> runConcurrently(int count, ThrowingFunction<Integer, Result> task) throws Exception {
        CountDownLatch ready = new CountDownLatch(count);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(count);
        try {
            List<Future<Result>> futures = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                int index = i;
                futures.add(executor.submit((Callable<Result>) () -> {
                    ready.countDown();
                    assertTrue(start.await(1, TimeUnit.SECONDS));
                    try {
                        return task.apply(index);
                    } catch (BookingInProgressException ex) {
                        return Result.BOOKING_IN_PROGRESS;
                    }
                }));
            }
            assertTrue(ready.await(1, TimeUnit.SECONDS));
            start.countDown();

            List<Result> results = new ArrayList<>();
            for (Future<Result> future : futures) {
                results.add(future.get(2, TimeUnit.SECONDS));
            }
            return results;
        } finally {
            executor.shutdownNow();
        }
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    private enum Result {
        SUCCESS,
        BOOKING_IN_PROGRESS
    }

    @FunctionalInterface
    private interface ThrowingFunction<T, R> {
        R apply(T value) throws Exception;
    }
}
