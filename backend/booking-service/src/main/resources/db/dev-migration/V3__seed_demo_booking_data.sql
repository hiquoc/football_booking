INSERT INTO sub_field_projections (id, field_id, field_name, name, active, owner_id, sub_field_type,
                                   minimum_booking_duration_minutes, maximum_booking_duration_minutes, booking_interval_minutes)
SELECT md5('demo-sub-field-' || n)::uuid, md5('demo-field-' || (((n - 1) / 2) + 1))::uuid,
       'Sân thể thao Demo ' || (((n - 1) / 2) + 1), 'Sân ' || CASE WHEN n % 2 = 1 THEN 'A' ELSE 'B' END, TRUE,
       ('20000000-0000-0000-0000-' || lpad((((((n - 1) / 2)) % 3) + 1)::text, 12, '0'))::uuid,
       CASE ((n - 1) / 2) % 5 WHEN 0 THEN CASE WHEN n % 2 = 1 THEN 'FOOTBALL_5V5' ELSE 'FOOTBALL_7V7' END
            WHEN 1 THEN 'BADMINTON' WHEN 2 THEN 'TENNIS' WHEN 3 THEN 'BASKETBALL_FULL_COURT' ELSE 'VOLLEYBALL' END,
       60, 180, 30 FROM generate_series(1, 30) AS n
ON CONFLICT (id) DO NOTHING;

INSERT INTO field_operating_hours_projections (id, field_id, day_of_week, open_time, close_time, closed)
SELECT md5('demo-booking-field-hours-' || f || '-' || d)::uuid, md5('demo-field-' || f)::uuid,
       (ARRAY['MONDAY','TUESDAY','WEDNESDAY','THURSDAY','FRIDAY','SATURDAY','SUNDAY'])[d], '06:00'::time, '23:00'::time, FALSE
FROM generate_series(1, 15) AS f CROSS JOIN generate_series(1, 7) AS d
ON CONFLICT (field_id, day_of_week) DO NOTHING;

INSERT INTO time_price_rule_projections (id, sub_field_id, start_time, end_time, hourly_price)
SELECT md5('demo-price-projection-' || n || '-' || slot.i)::uuid, md5('demo-sub-field-' || n)::uuid,
       slot.start_time, slot.end_time, slot.price + (((n - 1) / 2) * 10000)
FROM generate_series(1, 30) AS n
CROSS JOIN (VALUES (1, '06:00'::time, '17:00'::time, 120000::numeric),
                   (2, '17:00'::time, '23:00'::time, 180000::numeric)) slot(i, start_time, end_time, price)
ON CONFLICT (id) DO NOTHING;

INSERT INTO sub_field_closure_projections (id, sub_field_id, start_date, end_date, reason)
SELECT md5('demo-closure-' || n)::uuid, md5('demo-sub-field-' || n)::uuid,
       CURRENT_DATE + 14 + n, CURRENT_DATE + 14 + n, 'Bảo trì mặt sân định kỳ'
FROM generate_series(1, 5) AS n ON CONFLICT (id) DO NOTHING;

INSERT INTO bookings (id, booking_code, client_id, sub_field_id, owner_id, booking_date, start_time, end_time,
                      duration_minutes, price_per_hour, total_amount, status, note, cancellation_reason,
                      cancelled_at, cancelled_by, created_at, updated_at, created_by, updated_by, deleted)
SELECT md5('demo-booking-' || n)::uuid, 'DEMO-' || lpad(n::text, 5, '0'),
       ('30000000-0000-0000-0000-' || lpad((((n - 1) % 5) + 1)::text, 12, '0'))::uuid,
       md5('demo-sub-field-' || (((n - 1) % 30) + 1))::uuid,
       ('20000000-0000-0000-0000-' || lpad((((((n - 1) % 30) / 2) % 3) + 1)::text, 12, '0'))::uuid,
       CURRENT_DATE + CASE WHEN n <= 12 THEN -n ELSE 1 + ((n - 13) % 10) END,
       CASE WHEN n % 2 = 0 THEN '18:00'::time ELSE '08:00'::time END,
       CASE WHEN n % 2 = 0 THEN '19:30'::time ELSE '09:00'::time END,
       CASE WHEN n % 2 = 0 THEN 90 ELSE 60 END,
       CASE WHEN n % 2 = 0 THEN 180000 ELSE 120000 END,
       CASE WHEN n % 2 = 0 THEN 270000 ELSE 120000 END,
       CASE WHEN n <= 8 THEN 'COMPLETED' WHEN n <= 12 THEN 'CANCELLED'
            WHEN n % 4 = 0 THEN 'PENDING' ELSE 'CONFIRMED' END,
       'Dữ liệu đặt sân mẫu', CASE WHEN n BETWEEN 9 AND 12 THEN 'Thay đổi kế hoạch' ELSE NULL END,
       CASE WHEN n BETWEEN 9 AND 12 THEN NOW() - (n || ' hours')::interval ELSE NULL END,
       CASE WHEN n BETWEEN 9 AND 12 THEN 'CLIENT' ELSE NULL END,
       NOW() - (n || ' hours')::interval, NOW(), 'flyway', 'flyway', FALSE
FROM generate_series(1, 40) AS n
ON CONFLICT (id) DO NOTHING;
