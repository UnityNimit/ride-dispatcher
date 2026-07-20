package com.credx.dispatchhub.service;

import com.credx.dispatchhub.dto.response.DashboardStatsResponse;
import com.credx.dispatchhub.dto.response.DriverTripStatsResponse;
import com.credx.dispatchhub.enums.DriverStatus;
import com.credx.dispatchhub.enums.TripStatus;
import com.credx.dispatchhub.repository.DriverProfileRepository;
import com.credx.dispatchhub.repository.RiderProfileRepository;
import com.credx.dispatchhub.repository.TripRepository;
import com.credx.dispatchhub.util.DateTimeUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final TripRepository tripRepository;
    private final DriverProfileRepository driverProfileRepository;
    private final RiderProfileRepository riderProfileRepository;

    @Transactional(readOnly = true)
    public DashboardStatsResponse getDashboardStats() {
        Instant now = Instant.now();
        Instant startOfDay = DateTimeUtils.startOfDayUtc(now);
        Instant endOfDay = DateTimeUtils.endOfDayUtc(now);

        long totalTripsToday = tripRepository.countByRequestedAtBetween(startOfDay, endOfDay);
        long completedToday = tripRepository.countByStatusAndRequestedAtBetween(TripStatus.COMPLETED, startOfDay, endOfDay);
        long cancelledToday = tripRepository.countByStatusAndRequestedAtBetween(TripStatus.CANCELLED, startOfDay, endOfDay);
        long inProgress = tripRepository.findByStatusIn(List.of(TripStatus.IN_PROGRESS, TripStatus.ARRIVED)).size();
        long activeDrivers = driverProfileRepository.findByStatus(DriverStatus.AVAILABLE).size()
                + driverProfileRepository.findByStatus(DriverStatus.ON_TRIP).size();

        return DashboardStatsResponse.builder()
                .totalTripsToday(totalTripsToday)
                .activeDrivers(activeDrivers)
                .completedTripsToday(completedToday)
                .cancelledTripsToday(cancelledToday)
                .tripsInProgress(inProgress)
                .totalRegisteredDrivers(driverProfileRepository.count())
                .totalRegisteredRiders(riderProfileRepository.count())
                .build();
    }

    /**
     * Trips-per-driver analytics aggregated in the database (GROUP BY), not in Java memory.
     */
    @Transactional(readOnly = true)
    public List<DriverTripStatsResponse> getTripsPerDriver(Instant from, Instant to) {
        return tripRepository.getAggregatedDriverStats(from, to);
    }
}
