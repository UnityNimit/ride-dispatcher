-- Apply on existing databases that were created before the scalability indexes.
-- Safe to re-run (IF NOT EXISTS).

CREATE INDEX IF NOT EXISTS idx_driver_profiles_status_geo
    ON driver_profiles (status, current_lat, current_lng);

CREATE INDEX IF NOT EXISTS idx_trips_status_pickup_geo
    ON trips (status, pickup_lat, pickup_lng);
