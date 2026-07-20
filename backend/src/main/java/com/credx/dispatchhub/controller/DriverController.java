package com.credx.dispatchhub.controller;

import com.credx.dispatchhub.dto.request.DriverAvailabilityRequest;
import com.credx.dispatchhub.dto.request.DriverLocationUpdateRequest;
import com.credx.dispatchhub.dto.request.DriverProfileUpdateRequest;
import com.credx.dispatchhub.dto.response.DriverProfileResponse;
import com.credx.dispatchhub.dto.response.PageResponse;
import com.credx.dispatchhub.enums.DriverStatus;
import com.credx.dispatchhub.security.CurrentUser;
import com.credx.dispatchhub.service.DriverService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/drivers")
@RequiredArgsConstructor
public class DriverController {

    private final DriverService driverService;
    private final CurrentUser currentUser;

    @GetMapping
    public ResponseEntity<PageResponse<DriverProfileResponse>> listDrivers(
            @RequestParam(required = false) DriverStatus status,
            @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(PageResponse.of(driverService.listDrivers(status, pageable)));
    }

    @GetMapping("/nearby")
    public ResponseEntity<List<DriverProfileResponse>> findNearbyDrivers(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(defaultValue = "5") double radiusKm) {
        return ResponseEntity.ok(driverService.findNearbyAvailableDrivers(lat, lng, radiusKm));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DriverProfileResponse> getDriver(@PathVariable Long id) {
        return ResponseEntity.ok(driverService.getDriverById(id));
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<DriverProfileResponse> getMyDriverProfile() {
        return ResponseEntity.ok(driverService.getDriverByUserId(currentUser.id()));
    }

    @PatchMapping("/me/availability")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<DriverProfileResponse> updateAvailability(@Valid @RequestBody DriverAvailabilityRequest request) {
        return ResponseEntity.ok(driverService.updateAvailability(currentUser.id(), request));
    }

    @PatchMapping("/me/location")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<DriverProfileResponse> updateLocation(@Valid @RequestBody DriverLocationUpdateRequest request) {
        return ResponseEntity.ok(driverService.updateLocation(currentUser.id(), request));
    }

    @PutMapping("/me/profile")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<DriverProfileResponse> updateProfile(@Valid @RequestBody DriverProfileUpdateRequest request) {
        return ResponseEntity.ok(driverService.updateProfile(currentUser.id(), request));
    }
}
