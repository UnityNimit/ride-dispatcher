package com.credx.dispatchhub.controller;

import com.credx.dispatchhub.dto.request.CancelTripRequest;
import com.credx.dispatchhub.dto.request.CompleteTripRequest;
import com.credx.dispatchhub.dto.request.FareEstimateRequest;
import com.credx.dispatchhub.dto.request.ReviewRequest;
import com.credx.dispatchhub.dto.request.TripRequest;
import com.credx.dispatchhub.dto.response.FareEstimateResponse;
import com.credx.dispatchhub.dto.response.PageResponse;
import com.credx.dispatchhub.dto.response.ReviewResponse;
import com.credx.dispatchhub.dto.response.TripResponse;
import com.credx.dispatchhub.enums.TripStatus;
import com.credx.dispatchhub.security.CurrentUser;
import com.credx.dispatchhub.service.ReviewService;
import com.credx.dispatchhub.service.TripService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/trips")
@RequiredArgsConstructor
public class TripController {

    private final TripService tripService;
    private final ReviewService reviewService;
    private final CurrentUser currentUser;

    @PostMapping
    @PreAuthorize("hasRole('RIDER')")
    public ResponseEntity<TripResponse> requestTrip(@Valid @RequestBody TripRequest request) {
        TripResponse response = tripService.requestTrip(currentUser.id(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/estimate-fare")
    public ResponseEntity<FareEstimateResponse> estimateFare(@Valid @RequestBody FareEstimateRequest request) {
        return ResponseEntity.ok(tripService.estimateFare(
                request.pickupLat(), request.pickupLng(), request.dropoffLat(), request.dropoffLng()));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DRIVER')")
    public ResponseEntity<PageResponse<TripResponse>> listTrips(
            @RequestParam(required = false) TripStatus status,
            @PageableDefault(size = 20, sort = "requestedAt") Pageable pageable) {
        // Drivers may browse open REQUESTED trips for the incoming-request inbox.
        if ("DRIVER".equals(currentUser.get().getRole()) && status != TripStatus.REQUESTED) {
            status = TripStatus.REQUESTED;
        }
        return ResponseEntity.ok(PageResponse.of(tripService.listTrips(status, pageable)));
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('RIDER')")
    public ResponseEntity<PageResponse<TripResponse>> getMyTrips(
            @RequestParam(required = false) TripStatus status,
            @PageableDefault(size = 20, sort = "requestedAt") Pageable pageable) {
        return ResponseEntity.ok(PageResponse.of(tripService.listTripsForRider(currentUser.id(), status, pageable)));
    }

    @GetMapping("/driver/{driverProfileId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DRIVER')")
    public ResponseEntity<PageResponse<TripResponse>> getDriverTrips(
            @PathVariable Long driverProfileId,
            @PageableDefault(size = 20, sort = "requestedAt") Pageable pageable) {
        return ResponseEntity.ok(PageResponse.of(tripService.listTripsForDriver(driverProfileId, pageable)));
    }

    @GetMapping("/nearby")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<List<TripResponse>> findNearbyRequestedTrips(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(defaultValue = "10") double radiusKm) {
        return ResponseEntity.ok(tripService.findNearbyRequestedTrips(lat, lng, radiusKm));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('RIDER', 'DRIVER', 'ADMIN')")
    public ResponseEntity<TripResponse> getTrip(@PathVariable Long id) {
        return ResponseEntity.ok(tripService.getTripById(id, currentUser.id()));
    }

    @PostMapping("/{id}/accept")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<TripResponse> acceptTrip(@PathVariable Long id) {
        return ResponseEntity.ok(tripService.acceptTrip(id, currentUser.id()));
    }

    @PostMapping("/{id}/arrive")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<TripResponse> markArrived(@PathVariable Long id) {
        return ResponseEntity.ok(tripService.markArrived(id, currentUser.id()));
    }

    @PostMapping("/{id}/start")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<TripResponse> startTrip(@PathVariable Long id) {
        return ResponseEntity.ok(tripService.startTrip(id, currentUser.id()));
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<TripResponse> completeTrip(@PathVariable Long id,
                                                       @RequestBody(required = false) CompleteTripRequest request) {
        return ResponseEntity.ok(tripService.completeTrip(id, currentUser.id(), request));
    }

    // Riders cancel their own trips, drivers cancel trips assigned to them,
    // admins can cancel any non-terminal trip. Ownership is enforced in TripService.
    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('RIDER', 'DRIVER', 'ADMIN')")
    public ResponseEntity<TripResponse> cancelTrip(@PathVariable Long id,
                                                     @Valid @RequestBody(required = false) CancelTripRequest request) {
        return ResponseEntity.ok(tripService.cancelTrip(id, currentUser.id(), request));
    }

    @PostMapping("/{id}/review")
    @PreAuthorize("hasRole('RIDER')")
    public ResponseEntity<ReviewResponse> submitReview(@PathVariable Long id, @Valid @RequestBody ReviewRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reviewService.submitReview(id, currentUser.id(), request));
    }
}
