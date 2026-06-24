package com.project.booking.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BookingDatabaseConstraintInitializer implements ApplicationRunner {

    public static final String ACTIVE_BOOKING_OVERLAP_CONSTRAINT = "bookings_no_overlapping_active_bookings";

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        jdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS btree_gist");
        jdbcTemplate.execute("""
                DO $$
                BEGIN
                    IF NOT EXISTS (
                        SELECT 1
                        FROM pg_constraint
                        WHERE conname = 'bookings_no_overlapping_active_bookings'
                    ) THEN
                        ALTER TABLE bookings
                        ADD CONSTRAINT bookings_no_overlapping_active_bookings
                        EXCLUDE USING gist (
                            sub_field_id WITH =,
                            tsrange(booking_date + start_time, booking_date + end_time, '[)') WITH &&
                        )
                        WHERE (status IN ('PENDING', 'CONFIRMED') AND deleted = false);
                    END IF;
                END $$;
                """);
        log.info("Ensured PostgreSQL exclusion constraint {}", ACTIVE_BOOKING_OVERLAP_CONSTRAINT);
    }
}
