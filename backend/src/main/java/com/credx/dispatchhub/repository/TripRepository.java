package com.credx.dispatchhub.repository;

import com.credx.dispatchhub.dto.response.DriverTripStatsResponse;
import com.credx.dispatchhub.entity.Trip;
import com.credx.dispatchhub.enums.TripStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TripRepository extends JpaRepository<Trip, Long> {

    @EntityGraph(attributePaths = {"rider", "driver", "driver.user"})
    @Query("select t from Trip t")
    Page<Trip> findAllWithAssociations(Pageable pageable);

    @EntityGraph(attributePaths = {"rider", "driver", "driver.user"})
    @Query("select t from Trip t where t.status = :status")
    Page<Trip> findByStatusWithAssociations(@Param("status") TripStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"rider", "driver", "driver.user"})
    @Query("select t from Trip t where t.rider.id = :riderId")
    Page<Trip> findByRiderIdWithAssociations(@Param("riderId") Long riderId, Pageable pageable);

    @EntityGraph(attributePaths = {"rider", "driver", "driver.user"})
    @Query("select t from Trip t where t.rider.id = :riderId and t.status = :status")
    Page<Trip> findByRiderIdAndStatusWithAssociations(
            @Param("riderId") Long riderId,
            @Param("status") TripStatus status,
            Pageable pageable);

    @EntityGraph(attributePaths = {"rider", "driver", "driver.user"})
    @Query("select t from Trip t where t.driver.id = :driverId")
    Page<Trip> findByDriverIdWithAssociations(@Param("driverId") Long driverId, Pageable pageable);

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

    long countByStatusIn(Collection<TripStatus> statuses);

    List<Trip> findByStatusIn(List<TripStatus> statuses);

    @Query("""
            SELECT new com.credx.dispatchhub.dto.response.DriverTripStatsResponse(
                d.id,
                u.fullName,
                COUNT(t.id),
                COALESCE(SUM(COALESCE(t.finalFare, t.fareEstimate)), 0),
                COALESCE(AVG(COALESCE(t.finalFare, t.fareEstimate)), 0)
            )
            FROM Trip t
            JOIN t.driver d
            JOIN d.user u
            WHERE t.status = com.credx.dispatchhub.enums.TripStatus.COMPLETED
              AND t.requestedAt BETWEEN :from AND :to
            GROUP BY d.id, u.fullName
            """)
    List<DriverTripStatsResponse> getAggregatedDriverStats(@Param("from") Instant from, @Param("to") Instant to);

    @Query(value = """
            SELECT t.id FROM trips t
            WHERE t.status = 'REQUESTED'
              AND t.pickup_lat IS NOT NULL
              AND t.pickup_lng IS NOT NULL
              AND t.pickup_lat BETWEEN :minLat AND :maxLat
              AND t.pickup_lng BETWEEN :minLng AND :maxLng
              AND (6371 * acos(
                    LEAST(1.0, GREATEST(-1.0,
                      cos(radians(:lat)) * cos(radians(t.pickup_lat))
                      * cos(radians(t.pickup_lng) - radians(:lng))
                      + sin(radians(:lat)) * sin(radians(t.pickup_lat))
                    ))
                  )) <= :radiusKm
            ORDER BY (6371 * acos(
                    LEAST(1.0, GREATEST(-1.0,
                      cos(radians(:lat)) * cos(radians(t.pickup_lat))
                      * cos(radians(t.pickup_lng) - radians(:lng))
                      + sin(radians(:lat)) * sin(radians(t.pickup_lat))
                    ))
                  )) ASC
            LIMIT 20
            """, nativeQuery = true)
    List<Number> findNearbyRequestedTripIds(
            @Param("lat") double lat,
            @Param("lng") double lng,
            @Param("radiusKm") double radiusKm,
            @Param("minLat") double minLat,
            @Param("maxLat") double maxLat,
            @Param("minLng") double minLng,
            @Param("maxLng") double maxLng);

    @Query("""
            select distinct t from Trip t
            left join fetch t.rider
            left join fetch t.driver d
            left join fetch d.user
            where t.id in :ids
            """)
    List<Trip> findByIdInWithRiderAndDriver(@Param("ids") List<Long> ids);
}
