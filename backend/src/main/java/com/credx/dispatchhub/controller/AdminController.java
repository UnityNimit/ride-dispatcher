package com.credx.dispatchhub.controller;

import com.credx.dispatchhub.dto.response.DashboardStatsResponse;
import com.credx.dispatchhub.dto.response.DriverTripStatsResponse;
import com.credx.dispatchhub.dto.response.TripResponse;
import com.credx.dispatchhub.security.CurrentUser;
import com.credx.dispatchhub.service.AnalyticsService;
import com.credx.dispatchhub.service.TripService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {

    private final AnalyticsService analyticsService;
    private final TripService tripService;
    private final CurrentUser currentUser;

    @GetMapping("/dashboard-stats")
    public ResponseEntity<DashboardStatsResponse> getDashboardStats() {
        return ResponseEntity.ok(analyticsService.getDashboardStats());
    }

    @GetMapping("/analytics/trips-per-driver")
    public ResponseEntity<List<DriverTripStatsResponse>> getTripsPerDriver(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        Instant effectiveTo = to != null ? to : Instant.now();
        Instant effectiveFrom = from != null ? from : effectiveTo.minus(30, ChronoUnit.DAYS);
        return ResponseEntity.ok(analyticsService.getTripsPerDriver(effectiveFrom, effectiveTo));
    }

    @PostMapping("/trips/{id}/force-cancel")
    public ResponseEntity<TripResponse> forceCancelTrip(
            @PathVariable Long id,
            @RequestParam(required = false) String reason) {
        return ResponseEntity.ok(tripService.forceCancelTrip(id, currentUser.id(), reason));
    }
}
