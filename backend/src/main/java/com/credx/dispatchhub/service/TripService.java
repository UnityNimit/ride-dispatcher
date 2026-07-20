package com.credx.dispatchhub.service;

import com.credx.dispatchhub.dto.request.CancelTripRequest;
import com.credx.dispatchhub.dto.request.CompleteTripRequest;
import com.credx.dispatchhub.dto.request.TripRequest;
import com.credx.dispatchhub.dto.response.FareEstimateResponse;
import com.credx.dispatchhub.dto.response.TripResponse;
import com.credx.dispatchhub.dto.response.TripStatusHistoryResponse;
import com.credx.dispatchhub.entity.DriverProfile;
import com.credx.dispatchhub.entity.Trip;
import com.credx.dispatchhub.entity.TripStatusHistory;
import com.credx.dispatchhub.entity.User;
import com.credx.dispatchhub.enums.DriverStatus;
import com.credx.dispatchhub.enums.TripStatus;
import com.credx.dispatchhub.enums.UserRole;
import com.credx.dispatchhub.exception.DriverUnavailableException;
import com.credx.dispatchhub.exception.InvalidTripStateException;
import com.credx.dispatchhub.exception.ResourceNotFoundException;
import com.credx.dispatchhub.repository.DriverProfileRepository;
import com.credx.dispatchhub.repository.TripRepository;
import com.credx.dispatchhub.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TripService {

    private final TripRepository tripRepository;
    private final DriverProfileRepository driverProfileRepository;
    private final UserRepository userRepository;
    private final FareEstimationService fareEstimationService;

    @Transactional
    public TripResponse requestTrip(Long riderId, TripRequest request) {
        User rider = userRepository.findById(riderId)
                .orElseThrow(() -> new ResourceNotFoundException("Rider not found"));

        double distanceKm = fareEstimationService.distanceKm(
                request.pickupLat(), request.pickupLng(), request.dropoffLat(), request.dropoffLng());
        double durationMinutes = (distanceKm / 30.0) * 60.0;
        var fare = fareEstimationService.calculateFare(distanceKm, durationMinutes);

        Trip trip = Trip.builder()
                .rider(rider)
                .pickupLat(request.pickupLat())
                .pickupLng(request.pickupLng())
                .pickupAddress(request.pickupAddress())
                .dropoffLat(request.dropoffLat())
                .dropoffLng(request.dropoffLng())
                .dropoffAddress(request.dropoffAddress())
                .status(TripStatus.REQUESTED)
                .requestedAt(Instant.now())
                .fareEstimate(fare)
                .distanceKm(distanceKm)
                .build();

        trip.addStatusHistory(TripStatusHistory.builder()
                .status(TripStatus.REQUESTED)
                .changedAt(Instant.now())
                .build());

        trip = tripRepository.save(trip);
        return toResponse(trip);
    }

    @Transactional(readOnly = true)
    public FareEstimateResponse estimateFare(double pickupLat, double pickupLng, double dropoffLat, double dropoffLng) {
        return fareEstimationService.estimate(pickupLat, pickupLng, dropoffLat, dropoffLng);
    }

    @Transactional(readOnly = true)
    public Page<TripResponse> listTrips(TripStatus status, Pageable pageable) {
        Page<Trip> page = (status != null)
                ? tripRepository.findByStatus(status, pageable)
                : tripRepository.findAll(pageable);

        // page.map() iterates each row and calls toResponse(), which touches
        // trip.getRider() and trip.getDriver() - both FetchType.LAZY - so a
        // "page" of 20 trips means up to 41 additional SELECTs (1 for rider +
        // 1 for driver's user, per row) on top of the initial page query.
        // Enable SHOW_SQL to see it.
        return page.map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<TripResponse> listTripsForRider(Long riderId, TripStatus status, Pageable pageable) {
        Page<Trip> page = (status != null)
                ? tripRepository.findByRiderIdAndStatus(riderId, status, pageable)
                : tripRepository.findByRiderId(riderId, pageable);
        return page.map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<TripResponse> listTripsForDriver(Long driverProfileId, Pageable pageable) {
        return tripRepository.findByDriverId(driverProfileId, pageable).map(this::toResponse);
    }

    /**
     * Returns a single trip by id. Used by both the rider-facing trip detail
     * page and the admin dashboard, so it does NOT restrict by requester -
     * callers (controller layer) are expected to apply their own authorization
     * before exposing this to a non-admin caller.
     */
    @Transactional(readOnly = true)
    public TripResponse getTripById(Long tripId) {
        Trip trip = tripRepository.findByIdWithRiderAndDriver(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found with id: " + tripId));
        return toResponse(trip);
    }

    /**
     * Open REQUESTED trips near a point, nearest pickup first — filtered in SQL.
     */
    @Transactional(readOnly = true)
    public List<TripResponse> findNearbyRequestedTrips(double lat, double lng, double radiusKm) {
        if (radiusKm <= 0) {
            throw new IllegalArgumentException("radiusKm must be positive");
        }

        List<Long> orderedIds = tripRepository.findNearbyRequestedTripIds(lat, lng, radiusKm).stream()
                .map(Number::longValue)
                .toList();
        if (orderedIds.isEmpty()) {
            return List.of();
        }

        Map<Long, Trip> byId = tripRepository.findByIdInWithRiderAndDriver(orderedIds).stream()
                .collect(Collectors.toMap(Trip::getId, Function.identity()));

        List<TripResponse> results = new ArrayList<>(orderedIds.size());
        for (Long id : orderedIds) {
            Trip trip = byId.get(id);
            if (trip != null) {
                results.add(toResponse(trip));
            }
        }
        return results;
    }

    @Transactional
    public TripResponse acceptTrip(Long tripId, Long driverUserId) {
        // Pessimistic lock prevents two drivers from claiming the same REQUESTED trip.
        Trip trip = tripRepository.findByIdForUpdate(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found with id: " + tripId));

        if (trip.getStatus() != TripStatus.REQUESTED) {
            throw new InvalidTripStateException("Trip is no longer available to accept");
        }

        DriverProfile driver = driverProfileRepository.findByUserId(driverUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver profile not found"));

        if (driver.getStatus() != DriverStatus.AVAILABLE) {
            throw new DriverUnavailableException("Driver is not currently available");
        }

        // @Version on DriverProfile short-circuits concurrent accepts by the same
        // driver onto two different trips (OptimisticLockingFailureException → 409).
        driver.setStatus(DriverStatus.ON_TRIP);
        driverProfileRepository.saveAndFlush(driver);

        trip.setDriver(driver);
        trip.setStatus(TripStatus.ACCEPTED);
        trip.setAcceptedAt(Instant.now());
        trip.addStatusHistory(TripStatusHistory.builder()
                .status(TripStatus.ACCEPTED)
                .changedAt(Instant.now())
                .build());

        return toResponse(tripRepository.save(trip));
    }

    @Transactional
    public TripResponse markArrived(Long tripId, Long driverUserId) {
        Trip trip = getOwnedTripForDriver(tripId, driverUserId);

        if (trip.getStatus() != TripStatus.ACCEPTED) {
            throw new InvalidTripStateException("Trip must be ACCEPTED before the driver can arrive");
        }

        trip.setStatus(TripStatus.ARRIVED);
        trip.setArrivedAt(Instant.now());
        trip.addStatusHistory(TripStatusHistory.builder()
                .status(TripStatus.ARRIVED)
                .changedAt(Instant.now())
                .build());

        return toResponse(tripRepository.save(trip));
    }

    @Transactional
    public TripResponse startTrip(Long tripId, Long driverUserId) {
        Trip trip = getOwnedTripForDriver(tripId, driverUserId);

        if (trip.getStatus() != TripStatus.ARRIVED && trip.getStatus() != TripStatus.ACCEPTED) {
            throw new InvalidTripStateException("Trip must be ACCEPTED or ARRIVED before it can start");
        }

        trip.setStatus(TripStatus.IN_PROGRESS);
        trip.setStartedAt(Instant.now());
        trip.addStatusHistory(TripStatusHistory.builder()
                .status(TripStatus.IN_PROGRESS)
                .changedAt(Instant.now())
                .build());

        return toResponse(tripRepository.save(trip));
    }

    @Transactional
    public TripResponse completeTrip(Long tripId, Long driverUserId, CompleteTripRequest request) {
        Trip trip = getOwnedTripForDriver(tripId, driverUserId);

        if (trip.getStatus() != TripStatus.IN_PROGRESS) {
            throw new InvalidTripStateException("Trip must be IN_PROGRESS before it can be completed");
        }

        var finalFare = (request != null && request.finalFare() != null)
                ? request.finalFare()
                : trip.getFareEstimate();

        trip.setFinalFare(finalFare);
        trip.setStatus(TripStatus.COMPLETED);
        trip.setCompletedAt(Instant.now());
        trip.addStatusHistory(TripStatusHistory.builder()
                .status(TripStatus.COMPLETED)
                .changedAt(Instant.now())
                .build());

        DriverProfile driver = trip.getDriver();
        driver.setStatus(DriverStatus.AVAILABLE);
        driver.setTotalTrips(driver.getTotalTrips() + 1);
        driverProfileRepository.save(driver);

        return toResponse(tripRepository.save(trip));
    }

    /**
     * Cancels a trip. Riders can cancel their own trip; drivers can cancel a
     * trip assigned to them; admins can cancel any non-terminal trip.
     */
    @Transactional
    public TripResponse cancelTrip(Long tripId, Long requesterUserId, CancelTripRequest request) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found with id: " + tripId));

        assertCanCancel(trip, requesterUserId);

        if (trip.getStatus() == TripStatus.COMPLETED || trip.getStatus() == TripStatus.CANCELLED) {
            throw new InvalidTripStateException("Trip is already " + trip.getStatus());
        }

        return applyCancellation(trip, request != null ? request.reason() : null);
    }

    /**
     * Admin recovery path for trips stuck mid-lifecycle (driver app crash, etc.).
     */
    @Transactional
    public TripResponse forceCancelTrip(Long tripId, Long adminUserId, String reason) {
        User admin = userRepository.findById(adminUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (admin.getRole() != UserRole.ADMIN) {
            throw new AccessDeniedException("Only admins can force-cancel trips");
        }

        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found with id: " + tripId));

        if (trip.getStatus() == TripStatus.COMPLETED || trip.getStatus() == TripStatus.CANCELLED) {
            throw new InvalidTripStateException("Trip is already " + trip.getStatus());
        }

        String note = reason != null && !reason.isBlank()
                ? reason
                : "Force-cancelled by admin";
        return applyCancellation(trip, note);
    }

    /**
     * @deprecated superseded by {@link #listTripsForRider(Long, TripStatus, Pageable)}
     * which supports pagination and status filtering. Left in place because the
     * admin "rider trip history" export script still calls it directly.
     */
    @Deprecated
    @Transactional(readOnly = true)
    public List<TripResponse> getAllTripsForRiderUnpaged(Long riderId) {
        return tripRepository.findByRiderIdOrderByRequestedAtDesc(riderId).stream()
                .map(this::toResponse)
                .toList();
    }

    private void assertCanCancel(Trip trip, Long requesterUserId) {
        User requester = userRepository.findById(requesterUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (requester.getRole() == UserRole.ADMIN) {
            return;
        }
        if (requester.getRole() == UserRole.RIDER) {
            if (!trip.getRider().getId().equals(requesterUserId)) {
                throw new AccessDeniedException("You can only cancel your own trips");
            }
            return;
        }
        if (requester.getRole() == UserRole.DRIVER) {
            if (trip.getDriver() == null || !trip.getDriver().getUser().getId().equals(requesterUserId)) {
                throw new AccessDeniedException("You can only cancel trips assigned to you");
            }
            return;
        }
        throw new AccessDeniedException("You do not have permission to cancel this trip");
    }

    private TripResponse applyCancellation(Trip trip, String reason) {
        trip.setStatus(TripStatus.CANCELLED);
        trip.setCancelledAt(Instant.now());
        trip.setCancellationReason(reason);
        trip.addStatusHistory(TripStatusHistory.builder()
                .status(TripStatus.CANCELLED)
                .changedAt(Instant.now())
                .note(reason)
                .build());

        if (trip.getDriver() != null && trip.getDriver().getStatus() == DriverStatus.ON_TRIP) {
            DriverProfile driver = trip.getDriver();
            driver.setStatus(DriverStatus.AVAILABLE);
            driverProfileRepository.save(driver);
        }

        return toResponse(tripRepository.save(trip));
    }

    private Trip getOwnedTripForDriver(Long tripId, Long driverUserId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found with id: " + tripId));

        if (trip.getDriver() == null || !trip.getDriver().getUser().getId().equals(driverUserId)) {
            throw new InvalidTripStateException("This trip is not assigned to you");
        }
        return trip;
    }

    private TripResponse toResponse(Trip trip) {
        DriverProfile driver = trip.getDriver();

        List<TripStatusHistoryResponse> history = trip.getStatusHistory().stream()
                .map(h -> TripStatusHistoryResponse.builder()
                        .status(h.getStatus())
                        .changedAt(h.getChangedAt())
                        .note(h.getNote())
                        .build())
                .toList();

        return TripResponse.builder()
                .id(trip.getId())
                .riderId(trip.getRider().getId())
                .riderName(trip.getRider().getFullName())
                .driverId(driver != null ? driver.getId() : null)
                .driverName(driver != null ? driver.getUser().getFullName() : null)
                .driverVehicle(driver != null ? (driver.getVehicleMake() + " " + driver.getVehicleModel()) : null)
                .driverLicensePlate(driver != null ? driver.getLicensePlate() : null)
                .pickupLat(trip.getPickupLat())
                .pickupLng(trip.getPickupLng())
                .pickupAddress(trip.getPickupAddress())
                .dropoffLat(trip.getDropoffLat())
                .dropoffLng(trip.getDropoffLng())
                .dropoffAddress(trip.getDropoffAddress())
                .status(trip.getStatus())
                .requestedAt(trip.getRequestedAt())
                .acceptedAt(trip.getAcceptedAt())
                .arrivedAt(trip.getArrivedAt())
                .startedAt(trip.getStartedAt())
                .completedAt(trip.getCompletedAt())
                .cancelledAt(trip.getCancelledAt())
                .cancellationReason(trip.getCancellationReason())
                .fareEstimate(trip.getFareEstimate())
                .finalFare(trip.getFinalFare())
                .distanceKm(trip.getDistanceKm())
                .statusHistory(history)
                .build();
    }
}
