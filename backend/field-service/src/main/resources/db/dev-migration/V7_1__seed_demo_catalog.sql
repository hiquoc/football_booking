INSERT INTO field_types (sport_type, default_booking_duration_minutes, description, active,
                         created_at, updated_at, created_by, updated_by, deleted)
VALUES
 ('FOOTBALL', 60, 'Sân bóng đá', TRUE, NOW(), NOW(), 'flyway', 'flyway', FALSE),
 ('BASKETBALL', 60, 'Sân bóng rổ', TRUE, NOW(), NOW(), 'flyway', 'flyway', FALSE),
 ('BADMINTON', 60, 'Sân cầu lông', TRUE, NOW(), NOW(), 'flyway', 'flyway', FALSE),
 ('VOLLEYBALL', 60, 'Sân bóng chuyền', TRUE, NOW(), NOW(), 'flyway', 'flyway', FALSE),
 ('TENNIS', 60, 'Sân quần vợt', TRUE, NOW(), NOW(), 'flyway', 'flyway', FALSE)
ON CONFLICT (sport_type) DO NOTHING;

INSERT INTO fields (id, owner_id, name, description, address, ward, ward_code, province, province_code,
                    legacy_ward, legacy_ward_code, legacy_district, legacy_province, latitude, longitude,
                    phone_number, email, active, status, rating_average, total_reviews,
                    created_at, updated_at, created_by, updated_by, deleted)
SELECT md5('demo-field-' || n)::uuid,
       ('20000000-0000-0000-0000-' || lpad((((n - 1) % 3) + 1)::text, 12, '0'))::uuid,
       (ARRAY['Arena Thủ Đức','Green Field Phú Nhuận','Sport Hub Bình Thạnh','Victory Court Quận 7','Sunrise Sports Tân Bình',
              'Champion Park Gò Vấp','Riverside Arena','Galaxy Sports Center','Olympic Field','Golden Goal Arena',
              'Premier Court','City Sports Club','Diamond Field','Saigon Active','Unity Sports Park'])[n],
       'Cụm sân thể thao chất lượng cao, có đèn chiếu sáng, bãi xe và khu thay đồ.',
       (10 + n) || ' Đường Thể Thao', 'Phường ' || (((n - 1) % 10) + 1), 'WARD-' || n,
       CASE WHEN n <= 12 THEN 'Thành phố Hồ Chí Minh' ELSE 'Hà Nội' END,
       CASE WHEN n <= 12 THEN '79' ELSE '01' END,
       'Phường ' || (((n - 1) % 10) + 1), 'LEGACY-WARD-' || n,
       'Quận ' || (((n - 1) % 7) + 1), CASE WHEN n <= 12 THEN 'TP. Hồ Chí Minh' ELSE 'Hà Nội' END,
       CASE WHEN n <= 12 THEN 10.730000 + n * 0.008 ELSE 21.010000 + n * 0.002 END,
       CASE WHEN n <= 12 THEN 106.650000 + n * 0.009 ELSE 105.800000 + n * 0.002 END,
       '0287300' || lpad(n::text, 4, '0'), 'field' || n || '@football.local', TRUE,
       CASE WHEN n <= 12 THEN 'APPROVED' WHEN n <= 14 THEN 'PENDING' ELSE 'REJECTED' END,
       0, 0, NOW() - (n || ' days')::interval, NOW(), 'flyway', 'flyway', FALSE
FROM generate_series(1, 15) AS n
ON CONFLICT (id) DO NOTHING;

INSERT INTO field_field_types (field_id, field_type_id)
SELECT md5('demo-field-' || n)::uuid, ft.id
FROM generate_series(1, 15) AS n
JOIN field_types ft ON ft.sport_type = (ARRAY['FOOTBALL','BADMINTON','TENNIS','BASKETBALL','VOLLEYBALL'])[1 + ((n - 1) % 5)]
ON CONFLICT DO NOTHING;

INSERT INTO sub_fields (id, field_id, name, description, active, indoor_outdoor, surface_type, sub_field_type,
                        changing_room, shower, wifi, air_conditioning,
                        created_at, updated_at, created_by, updated_by, deleted)
SELECT md5('demo-sub-field-' || n)::uuid, md5('demo-field-' || (((n - 1) / 2) + 1))::uuid,
       'Sân ' || CASE WHEN n % 2 = 1 THEN 'A' ELSE 'B' END,
       'Sân con tiêu chuẩn, bảo trì định kỳ.', TRUE,
       CASE WHEN n % 3 = 0 THEN 'INDOOR' ELSE 'OUTDOOR' END,
       CASE ((n - 1) / 2) % 5 WHEN 0 THEN 'ARTIFICIAL_GRASS' WHEN 1 THEN 'WOODEN_FLOOR'
            WHEN 2 THEN 'CLAY_COURT' WHEN 3 THEN 'RUBBER_COURT' ELSE 'CONCRETE' END,
       CASE ((n - 1) / 2) % 5 WHEN 0 THEN CASE WHEN n % 2 = 1 THEN 'FOOTBALL_5V5' ELSE 'FOOTBALL_7V7' END
            WHEN 1 THEN 'BADMINTON' WHEN 2 THEN 'TENNIS' WHEN 3 THEN 'BASKETBALL_FULL_COURT' ELSE 'VOLLEYBALL' END,
       TRUE, n % 2 = 0, TRUE, n % 3 = 0, NOW(), NOW(), 'flyway', 'flyway', FALSE
FROM generate_series(1, 30) AS n
ON CONFLICT (id) DO NOTHING;

INSERT INTO booking_rules (sub_field_id, minimum_booking_duration_minutes, maximum_booking_duration_minutes, booking_interval_minutes)
SELECT md5('demo-sub-field-' || n)::uuid, 60, 180, 30 FROM generate_series(1, 30) AS n
ON CONFLICT (sub_field_id) DO NOTHING;

INSERT INTO time_price_rules (sub_field_id, start_time, end_time, hourly_price,
                              created_at, updated_at, created_by, updated_by, deleted)
SELECT md5('demo-sub-field-' || n)::uuid, slot.start_time, slot.end_time,
       slot.price + (((n - 1) / 2) * 10000), NOW(), NOW(), 'flyway', 'flyway', FALSE
FROM generate_series(1, 30) AS n
CROSS JOIN (VALUES ('06:00'::time, '17:00'::time, 120000::numeric),
                   ('17:00'::time, '23:00'::time, 180000::numeric)) slot(start_time, end_time, price);

INSERT INTO field_operating_hours (id, field_id, day_of_week, open_time, close_time, closed)
SELECT md5('demo-field-hours-' || f || '-' || d)::uuid, md5('demo-field-' || f)::uuid,
       (ARRAY['MONDAY','TUESDAY','WEDNESDAY','THURSDAY','FRIDAY','SATURDAY','SUNDAY'])[d],
       '06:00'::time, '23:00'::time, FALSE
FROM generate_series(1, 15) AS f CROSS JOIN generate_series(1, 7) AS d
ON CONFLICT (field_id, day_of_week) DO NOTHING;

INSERT INTO field_images (field_id, image_url, is_primary, display_order)
SELECT md5('demo-field-' || f)::uuid, images.urls[1 + ((f + image_no - 2) % 5)], image_no = 1, image_no - 1
FROM generate_series(1, 15) AS f CROSS JOIN generate_series(1, 3) AS image_no
CROSS JOIN (SELECT ARRAY[
 'https://res.cloudinary.com/dtvs3rgbw/image/upload/v1782904488/z9tbzetrekaxr3adgdoj.jpg',
 'https://res.cloudinary.com/dtvs3rgbw/image/upload/v1782904491/vj1hosvcdmqonbrkfwcm.jpg',
 'https://res.cloudinary.com/dtvs3rgbw/image/upload/v1782904491/vj1hosvcdmqonbrkfwcm.jpg',
 'https://res.cloudinary.com/dtvs3rgbw/image/upload/v1782904491/vj1hosvcdmqonbrkfwcm.jpg',
 'https://res.cloudinary.com/dtvs3rgbw/image/upload/v1782712257/Untitled_design_tsqdxg.png']::text[] AS urls) images;

INSERT INTO reviews (id, field_id, user_id, rating, comment, created_at, updated_at, created_by, updated_by, deleted)
SELECT md5('demo-review-' || n)::uuid, md5('demo-field-' || (((n - 1) % 12) + 1))::uuid,
       ('30000000-0000-0000-0000-' || lpad((((n - 1) % 5) + 1)::text, 12, '0'))::uuid,
       3 + (n % 3), (ARRAY['Sân sạch và nhân viên hỗ trợ tốt.','Mặt sân ổn, giá hợp lý.','Vị trí dễ tìm và đặt sân nhanh.'])[1 + (n % 3)],
       NOW() - (n || ' hours')::interval, NOW(), 'flyway', 'flyway', FALSE
FROM generate_series(1, 60) AS n
ON CONFLICT (id) DO NOTHING;

UPDATE fields f SET rating_average = stats.average_rating, total_reviews = stats.review_count
FROM (SELECT field_id, ROUND(AVG(rating), 2) average_rating, COUNT(*) review_count FROM reviews WHERE deleted = FALSE GROUP BY field_id) stats
WHERE f.id = stats.field_id;

INSERT INTO sub_field_closures (id, sub_field_id, start_date, end_date, reason)
SELECT md5('demo-closure-' || n)::uuid, md5('demo-sub-field-' || n)::uuid,
       CURRENT_DATE + 14 + n, CURRENT_DATE + 14 + n, 'Bảo trì mặt sân định kỳ'
FROM generate_series(1, 5) AS n ON CONFLICT (id) DO NOTHING;
