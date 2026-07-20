package com.credx.dispatchhub.repository;

import com.credx.dispatchhub.entity.DriverProfile;
import com.credx.dispatchhub.enums.DriverStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface DriverProfileRepository extends JpaRepository<DriverProfile, Long> {

    Optional<DriverProfile> findByUserId(Long userId);

    Page<DriverProfile> findByStatus(DriverStatus status, Pageable pageable);

    List<DriverProfile> findByStatus(DriverStatus status);

    @Query("select d from DriverProfile d join fetch d.user where d.id = :id")
    Optional<DriverProfile> findByIdWithUser(@Param("id") Long id);

    @Query(value = """
            SELECT d.id FROM driver_profiles d
            WHERE d.status = 'AVAILABLE'
              AND d.current_lat IS NOT NULL
              AND d.current_lng IS NOT NULL
              AND (6371 * acos(
                    LEAST(1.0, GREATEST(-1.0,
                      cos(radians(:lat)) * cos(radians(d.current_lat))
                      * cos(radians(d.current_lng) - radians(:lng))
                      + sin(radians(:lat)) * sin(radians(d.current_lat))
                    ))
                  )) <= :radiusKm
            ORDER BY (6371 * acos(
                    LEAST(1.0, GREATEST(-1.0,
                      cos(radians(:lat)) * cos(radians(d.current_lat))
                      * cos(radians(d.current_lng) - radians(:lng))
                      + sin(radians(:lat)) * sin(radians(d.current_lat))
                    ))
                  )) ASC
            LIMIT 20
            """, nativeQuery = true)
    List<Number> findNearbyAvailableDriverIds(
            @Param("lat") double lat,
            @Param("lng") double lng,
            @Param("radiusKm") double radiusKm);

    @Query("select d from DriverProfile d join fetch d.user where d.id in :ids")
    List<DriverProfile> findByIdInWithUser(@Param("ids") Collection<Long> ids);
}
