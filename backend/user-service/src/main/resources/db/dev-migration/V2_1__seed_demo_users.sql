INSERT INTO users (id, phone_number, email, full_name, avatar_url, user_type, status,
                   created_at, updated_at, created_by, updated_by, deleted)
VALUES
 ('10000000-0000-0000-0000-000000000001', '0900000001', 'admin@football.local', 'Quản trị viên Demo', NULL, 'ADMIN', 'ACTIVE', NOW(), NOW(), 'flyway', 'flyway', FALSE),
 ('20000000-0000-0000-0000-000000000001', '0900000011', 'owner1@football.local', 'Nguyễn Minh Sân', NULL, 'OWNER', 'ACTIVE', NOW(), NOW(), 'flyway', 'flyway', FALSE),
 ('20000000-0000-0000-0000-000000000002', '0900000012', 'owner2@football.local', 'Trần Hoàng Phát', NULL, 'OWNER', 'ACTIVE', NOW(), NOW(), 'flyway', 'flyway', FALSE),
 ('20000000-0000-0000-0000-000000000003', '0900000013', 'owner3@football.local', 'Lê Thanh Thể Thao', NULL, 'OWNER', 'ACTIVE', NOW(), NOW(), 'flyway', 'flyway', FALSE),
 ('30000000-0000-0000-0000-000000000001', '0900000021', 'client1@football.local', 'Phạm Anh Khoa', NULL, 'CLIENT', 'ACTIVE', NOW(), NOW(), 'flyway', 'flyway', FALSE),
 ('30000000-0000-0000-0000-000000000002', '0900000022', 'client2@football.local', 'Võ Ngọc Lan', NULL, 'CLIENT', 'ACTIVE', NOW(), NOW(), 'flyway', 'flyway', FALSE),
 ('30000000-0000-0000-0000-000000000003', '0900000023', 'client3@football.local', 'Đặng Quốc Bảo', NULL, 'CLIENT', 'ACTIVE', NOW(), NOW(), 'flyway', 'flyway', FALSE),
 ('30000000-0000-0000-0000-000000000004', '0900000024', 'client4@football.local', 'Bùi Gia Huy', NULL, 'CLIENT', 'ACTIVE', NOW(), NOW(), 'flyway', 'flyway', FALSE),
 ('30000000-0000-0000-0000-000000000005', '0900000025', 'client5@football.local', 'Đỗ Khánh Linh', NULL, 'CLIENT', 'ACTIVE', NOW(), NOW(), 'flyway', 'flyway', FALSE)
ON CONFLICT (id) DO NOTHING;
