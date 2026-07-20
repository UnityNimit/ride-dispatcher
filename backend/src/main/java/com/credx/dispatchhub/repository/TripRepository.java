package com.credx.dispatchhub.repository;

import com.credx.dispatchhub.entity.Trip;
import com.credx.dispatchhub.enums.TripStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface TripRepository extends JpaRepository<Trip, Long> {

    // NOTE: this does not eagerly join rider/driver, so anything that iterates
    // the page and touches trip.getRider()/trip.getDriver() will trigger a
    // separate SELECT per row (see TripService#toResponseList).
    Page<Trip> findAll(Pageable pageable);

    Page<Trip> findByStatus(TripStatus status, Pageable pageable);

    Page<Trip> findByRiderId(Long riderId, Pageable pageable);

    Page<Trip> findByRiderIdAndStatus(Long riderId, TripStatus status, Pageable pageable);

    Page<Trip> findByDriverId(Long driverId, Pageable pageable);

    List<Trip> findByRiderIdOrderByRequestedAtDesc(Long riderId);

    @Query("select t from Trip t left join fetch t.rider left join fetch t.driver d left join fetch d.user where t.id = :id")
    Optional<Trip> findByIdWithRiderAndDriver(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from Trip t where t.id = :id")
    Optional<Trip> findByIdForUpdate(@Param("id") Long id);

    long countByStatusAndRequestedAtBetween(TripStatus status, Instant from, Instant to);

    long countByRequestedAtBetween(Instant from, Instant to);

    List<Trip> findByStatusIn(List<TripStatus> statuses);

    // Used by the admin analytics endpoint. Intentionally simple - pulls every
    // trip row for the period into memory so the service layer can aggregate
    // "trips per driver". Fine for the seed dataset, not fine at scale.
    @Query("select t from Trip t where t.requestedAt between :from and :to")
    List<Trip> findAllForAnalytics(@Param("from") Instant from, @Param("to") Instant to);
}
