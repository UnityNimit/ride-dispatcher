package com.credx.dispatchhub.repository;

import com.credx.dispatchhub.entity.DriverProfile;
import com.credx.dispatchhub.enums.DriverStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DriverProfileRepository extends JpaRepository<DriverProfile, Long> {

    Optional<DriverProfile> findByUserId(Long userId);

    Page<DriverProfile> findByStatus(DriverStatus status, Pageable pageable);

    List<DriverProfile> findByStatus(DriverStatus status);

    @Query("select d from DriverProfile d join fetch d.user where d.id = :id")
    Optional<DriverProfile> findByIdWithUser(@Param("id") Long id);

    @Query("""
            select d from DriverProfile d join fetch d.user
            where d.status = com.credx.dispatchhub.enums.DriverStatus.AVAILABLE
              and d.currentLat is not null and d.currentLng is not null
            """)
    List<DriverProfile> findAllAvailableForNearbySearch();
}
