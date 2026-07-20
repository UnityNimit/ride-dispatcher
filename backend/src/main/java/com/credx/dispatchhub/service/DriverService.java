package com.credx.dispatchhub.service;

import com.credx.dispatchhub.dto.request.DriverAvailabilityRequest;
import com.credx.dispatchhub.dto.request.DriverLocationUpdateRequest;
import com.credx.dispatchhub.dto.request.DriverProfileUpdateRequest;
import com.credx.dispatchhub.dto.response.DriverProfileResponse;
import com.credx.dispatchhub.entity.DriverProfile;
import com.credx.dispatchhub.enums.DriverStatus;
import com.credx.dispatchhub.exception.ResourceNotFoundException;
import com.credx.dispatchhub.repository.DriverProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DriverService {

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
     * Available drivers within {@code radiusKm}, nearest first — filtered/sorted in SQL.
     */
    @Transactional(readOnly = true)
    public List<DriverProfileResponse> findNearbyAvailableDrivers(double lat, double lng, double radiusKm) {
        if (radiusKm <= 0) {
            throw new IllegalArgumentException("radiusKm must be positive");
        }

        List<Long> orderedIds = driverProfileRepository.findNearbyAvailableDriverIds(lat, lng, radiusKm)
                .stream()
                .map(Number::longValue)
                .toList();
        if (orderedIds.isEmpty()) {
            return List.of();
        }

        Map<Long, DriverProfile> byId = driverProfileRepository.findByIdInWithUser(orderedIds).stream()
                .collect(Collectors.toMap(DriverProfile::getId, Function.identity()));

        List<DriverProfileResponse> results = new ArrayList<>(orderedIds.size());
        for (Long id : orderedIds) {
            DriverProfile driver = byId.get(id);
            if (driver != null) {
                results.add(toResponse(driver));
            }
        }
        return results;
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
