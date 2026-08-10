ALTER TABLE bookings
    ADD COLUMN opponent_id UUID;

CREATE INDEX idx_bookings_opponent ON bookings(opponent_id);
