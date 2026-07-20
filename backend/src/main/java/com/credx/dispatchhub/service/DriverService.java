package com.credx.dispatchhub.service;

import com.credx.dispatchhub.dto.request.DriverAvailabilityRequest;
import com.credx.dispatchhub.dto.request.DriverLocationUpdateRequest;
import com.credx.dispatchhub.dto.request.DriverProfileUpdateRequest;
import com.credx.dispatchhub.dto.response.DriverProfileResponse;
import com.credx.dispatchhub.entity.DriverProfile;
import com.credx.dispatchhub.enums.DriverStatus;
import com.credx.dispatchhub.exception.ResourceNotFoundException;
import com.credx.dispatchhub.repository.DriverProfileRepository;
import com.credx.dispatchhub.util.GeoUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DriverService {

    private static final int MAX_NEARBY_RESULTS = 20;

    private final DriverProfileRepository driverProfileRepository;

    @Transactional(readOnly = true)
    public Page<DriverProfileResponse> listDrivers(DriverStatus status, Pageable pageable) {
        Page<DriverProfile> page = (status != null)
                ? driverProfileRepository.findByStatus(status, pageable)
                : driverProfileRepository.findAll(pageable);

        // Each call to driver.getUser() below lazily triggers its own SELECT
        // since DriverProfile.user is FetchType.LAZY and the page query above
        // doesn't join it - fine at seed-data scale, not fine with a real
        // driver roster.
        return page.map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public DriverProfileResponse getDriverById(Long id) {
        DriverProfile driver = driverProfileRepository.findByIdWithUser(id)
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found with id: " + id));
        return toResponse(driver);
    }

    @Transactional(readOnly = true)
    public DriverProfileResponse getDriverByUserId(Long userId) {
        DriverProfile driver = driverProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver profile not found for this user"));
        return toResponse(driver);
    }

    @Transactional
    public DriverProfileResponse updateAvailability(Long userId, DriverAvailabilityRequest request) {
        DriverProfile driver = driverProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver profile not found for this user"));

        if (driver.getStatus() == DriverStatus.ON_TRIP && request.status() == DriverStatus.AVAILABLE) {
            // A driver mid-trip shouldn't be able to flip straight back to AVAILABLE;
            // trip completion is what does that transition.
            throw new IllegalArgumentException("Cannot go available while a trip is in progress");
        }

        driver.setStatus(request.status());
        return toResponse(driverProfileRepository.save(driver));
    }

    @Transactional
    public DriverProfileResponse updateLocation(Long userId, DriverLocationUpdateRequest request) {
        DriverProfile driver = driverProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver profile not found for this user"));

        driver.setCurrentLat(request.lat());
        driver.setCurrentLng(request.lng());
        return toResponse(driverProfileRepository.save(driver));
    }

    @Transactional
    public DriverProfileResponse updateProfile(Long userId, DriverProfileUpdateRequest request) {
        DriverProfile driver = driverProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver profile not found for this user"));

        driver.setVehicleMake(request.vehicleMake());
        driver.setVehicleModel(request.vehicleModel());
        driver.setVehicleColor(request.vehicleColor());
        driver.setLicensePlate(request.licensePlate());
        return toResponse(driverProfileRepository.save(driver));
    }

    /**
     * Available drivers within {@code radiusKm} of the given point, nearest first.
     */
    @Transactional(readOnly = true)
    public List<DriverProfileResponse> findNearbyAvailableDrivers(double lat, double lng, double radiusKm) {
        if (radiusKm <= 0) {
            throw new IllegalArgumentException("radiusKm must be positive");
        }

        // Rough bounding-box pre-filter (~111 km per degree latitude).
        double latDelta = radiusKm / 111.0;
        double lngDelta = radiusKm / (111.0 * Math.max(0.2, Math.cos(Math.toRadians(lat))));

        return driverProfileRepository.findAllAvailableForNearbySearch().stream()
                .filter(d -> d.getCurrentLat() >= lat - latDelta
                        && d.getCurrentLat() <= lat + latDelta
                        && d.getCurrentLng() >= lng - lngDelta
                        && d.getCurrentLng() <= lng + lngDelta)
                .map(d -> new NearbyCandidate(d, GeoUtils.distanceKm(lat, lng, d.getCurrentLat(), d.getCurrentLng())))
                .filter(c -> c.distanceKm() <= radiusKm)
                .sorted(Comparator.comparingDouble(NearbyCandidate::distanceKm))
                .limit(MAX_NEARBY_RESULTS)
                .map(c -> toResponse(c.driver()))
                .toList();
    }

    private record NearbyCandidate(DriverProfile driver, double distanceKm) {
    }

    private DriverProfileResponse toResponse(DriverProfile driver) {
        return DriverProfileResponse.builder()
                .id(driver.getId())
                .userId(driver.getUser().getId())
                .fullName(driver.getUser().getFullName())
                .email(driver.getUser().getEmail())
                .phoneNumber(driver.getUser().getPhoneNumber())
                .vehicleMake(driver.getVehicleMake())
                .vehicleModel(driver.getVehicleModel())
                .vehicleColor(driver.getVehicleColor())
                .licensePlate(driver.getLicensePlate())
                .status(driver.getStatus())
                .currentLat(driver.getCurrentLat())
                .currentLng(driver.getCurrentLng())
                .rating(driver.getRating())
                .totalTrips(driver.getTotalTrips())
                .build();
    }
}
