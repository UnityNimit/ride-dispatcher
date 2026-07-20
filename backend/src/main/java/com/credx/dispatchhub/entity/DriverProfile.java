package com.credx.dispatchhub.entity;

import com.credx.dispatchhub.enums.DriverStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "driver_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DriverProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "vehicle_make", nullable = false)
    private String vehicleMake;

    @Column(name = "vehicle_model", nullable = false)
    private String vehicleModel;

    @Column(name = "vehicle_color")
    private String vehicleColor;

    @Column(name = "license_plate", nullable = false)
    private String licensePlate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private DriverStatus status = DriverStatus.OFFLINE;

    @Column(name = "current_lat")
    private Double currentLat;

    @Column(name = "current_lng")
    private Double currentLng;

    @Column(nullable = false)
    @Builder.Default
    private BigDecimal rating = BigDecimal.valueOf(5.0);

    @Column(name = "total_trips", nullable = false)
    @Builder.Default
    private int totalTrips = 0;

    // Optimistic locking to guard against the classic "same driver accepts two
    // trips concurrently" race. TripService.acceptTrip saves with flush so a
    // conflicting update surfaces as OptimisticLockingFailureException (409).
    @Version
    @Column(name = "version")
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
