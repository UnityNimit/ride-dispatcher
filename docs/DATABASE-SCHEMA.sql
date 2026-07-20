-- DispatchHub database schema (PostgreSQL)
-- Mirrors the JPA entities under backend/src/main/java/com/credx/dispatchhub/entity
-- Hibernate is configured with ddl-auto=update for local dev convenience, but
-- this file is the source of truth for the intended schema and is what
-- seed-data/seed.sql assumes exists.

-- =========================================================================
-- users
-- =========================================================================
CREATE TABLE IF NOT EXISTS users (
    id              BIGSERIAL PRIMARY KEY,
    email           VARCHAR(255) NOT NULL,
    password_hash   VARCHAR(255) NOT NULL,
    full_name       VARCHAR(150) NOT NULL,
    phone_number    VARCHAR(30),
    role            VARCHAR(20) NOT NULL CHECK (role IN ('RIDER', 'DRIVER', 'ADMIN')),
    enabled         BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_users_email UNIQUE (email)
);

-- =========================================================================
-- driver_profiles
-- =========================================================================
CREATE TABLE IF NOT EXISTS driver_profiles (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL,
    vehicle_make    VARCHAR(100) NOT NULL,
    vehicle_model   VARCHAR(100) NOT NULL,
    vehicle_color   VARCHAR(50),
    license_plate   VARCHAR(20) NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'OFFLINE' CHECK (status IN ('OFFLINE', 'AVAILABLE', 'ON_TRIP')),
    current_lat     DOUBLE PRECISION,
    current_lng     DOUBLE PRECISION,
    rating          NUMERIC(3, 2) NOT NULL DEFAULT 5.0,
    total_trips     INTEGER NOT NULL DEFAULT 0,
    version         BIGINT NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_driver_profiles_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT uk_driver_profiles_user UNIQUE (user_id)
);

CREATE INDEX IF NOT EXISTS idx_driver_profiles_status ON driver_profiles (status);
CREATE INDEX IF NOT EXISTS idx_driver_profiles_status_geo ON driver_profiles (status, current_lat, current_lng);

-- =========================================================================
-- rider_profiles
-- =========================================================================
CREATE TABLE IF NOT EXISTS rider_profiles (
    id                      BIGSERIAL PRIMARY KEY,
    user_id                 BIGINT NOT NULL,
    payment_method_label    VARCHAR(50),
    payment_method_last4    VARCHAR(4),
    rating                  NUMERIC(3, 2) NOT NULL DEFAULT 5.0,
    total_trips             INTEGER NOT NULL DEFAULT 0,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_rider_profiles_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT uk_rider_profiles_user UNIQUE (user_id)
);

-- =========================================================================
-- trips
-- =========================================================================
CREATE TABLE IF NOT EXISTS trips (
    id                      BIGSERIAL PRIMARY KEY,
    rider_id                BIGINT NOT NULL,
    driver_id               BIGINT,
    pickup_lat              DOUBLE PRECISION NOT NULL,
    pickup_lng              DOUBLE PRECISION NOT NULL,
    pickup_address          VARCHAR(255) NOT NULL,
    dropoff_lat             DOUBLE PRECISION NOT NULL,
    dropoff_lng             DOUBLE PRECISION NOT NULL,
    dropoff_address         VARCHAR(255) NOT NULL,
    status                  VARCHAR(20) NOT NULL DEFAULT 'REQUESTED'
                                CHECK (status IN ('REQUESTED', 'ACCEPTED', 'ARRIVED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED')),
    requested_at            TIMESTAMPTZ NOT NULL,
    accepted_at             TIMESTAMPTZ,
    arrived_at              TIMESTAMPTZ,
    started_at              TIMESTAMPTZ,
    completed_at            TIMESTAMPTZ,
    cancelled_at            TIMESTAMPTZ,
    cancellation_reason     VARCHAR(255),
    fare_estimate           NUMERIC(10, 2),
    final_fare              NUMERIC(10, 2),
    distance_km             DOUBLE PRECISION,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_trips_rider FOREIGN KEY (rider_id) REFERENCES users (id),
    CONSTRAINT fk_trips_driver FOREIGN KEY (driver_id) REFERENCES driver_profiles (id)
);

CREATE INDEX IF NOT EXISTS idx_trips_status ON trips (status);
CREATE INDEX IF NOT EXISTS idx_trips_rider_id ON trips (rider_id);
CREATE INDEX IF NOT EXISTS idx_trips_driver_id ON trips (driver_id);
CREATE INDEX IF NOT EXISTS idx_trips_requested_at ON trips (requested_at);
CREATE INDEX IF NOT EXISTS idx_trips_status_pickup_geo ON trips (status, pickup_lat, pickup_lng);

-- =========================================================================
-- trip_status_history
-- =========================================================================
CREATE TABLE IF NOT EXISTS trip_status_history (
    id              BIGSERIAL PRIMARY KEY,
    trip_id         BIGINT NOT NULL,
    status          VARCHAR(20) NOT NULL CHECK (status IN ('REQUESTED', 'ACCEPTED', 'ARRIVED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED')),
    changed_at      TIMESTAMPTZ NOT NULL,
    note            VARCHAR(255),
    CONSTRAINT fk_trip_status_history_trip FOREIGN KEY (trip_id) REFERENCES trips (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_trip_status_history_trip_id ON trip_status_history (trip_id);

-- =========================================================================
-- reviews
-- =========================================================================
CREATE TABLE IF NOT EXISTS reviews (
    id              BIGSERIAL PRIMARY KEY,
    trip_id         BIGINT NOT NULL,
    rider_id        BIGINT NOT NULL,
    driver_id       BIGINT NOT NULL,
    rating          INTEGER NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment         VARCHAR(1000),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_reviews_trip FOREIGN KEY (trip_id) REFERENCES trips (id) ON DELETE CASCADE,
    CONSTRAINT fk_reviews_rider FOREIGN KEY (rider_id) REFERENCES users (id),
    CONSTRAINT fk_reviews_driver FOREIGN KEY (driver_id) REFERENCES driver_profiles (id),
    CONSTRAINT uk_reviews_trip UNIQUE (trip_id)
);
