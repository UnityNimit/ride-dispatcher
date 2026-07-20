package com.credx.dispatchhub.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Getter
@Builder
@NoArgsConstructor
public class DriverTripStatsResponse {
    private Long driverId;
    private String driverName;
    private long completedTrips;
    private BigDecimal totalRevenue;
    private BigDecimal averageFare;

    /**
     * JPQL {@code SELECT NEW} constructor. Accepts Hibernate aggregate types
     * ({@link Long} for COUNT, {@link BigDecimal}/{@link Double}/{@link Number} for SUM/AVG).
     */
    public DriverTripStatsResponse(
            Long driverId,
            String driverName,
            Long completedTrips,
            Number totalRevenue,
            Number averageFare) {
        this.driverId = driverId;
        this.driverName = driverName;
        this.completedTrips = completedTrips != null ? completedTrips : 0L;
        this.totalRevenue = toMoney(totalRevenue);
        this.averageFare = toMoney(averageFare);
    }

    private static BigDecimal toMoney(Number value) {
        if (value == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        if (value instanceof BigDecimal bd) {
            return bd.setScale(2, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(value.doubleValue()).setScale(2, RoundingMode.HALF_UP);
    }
}
