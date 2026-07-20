package com.credx.dispatchhub.service;

import com.credx.dispatchhub.dto.request.ReviewRequest;
import com.credx.dispatchhub.dto.response.ReviewResponse;
import com.credx.dispatchhub.entity.DriverProfile;
import com.credx.dispatchhub.entity.Review;
import com.credx.dispatchhub.entity.Trip;
import com.credx.dispatchhub.enums.TripStatus;
import com.credx.dispatchhub.exception.DuplicateResourceException;
import com.credx.dispatchhub.exception.InvalidTripStateException;
import com.credx.dispatchhub.exception.ResourceNotFoundException;
import com.credx.dispatchhub.repository.DriverProfileRepository;
import com.credx.dispatchhub.repository.ReviewRepository;
import com.credx.dispatchhub.repository.TripRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final TripRepository tripRepository;
    private final DriverProfileRepository driverProfileRepository;

    @Transactional
    public ReviewResponse submitReview(Long tripId, Long riderId, ReviewRequest request) {
        Trip trip = tripRepository.findByIdWithRiderAndDriver(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found with id: " + tripId));

        if (!trip.getRider().getId().equals(riderId)) {
            throw new AccessDeniedException("You can only review your own trips");
        }
        if (trip.getStatus() != TripStatus.COMPLETED) {
            throw new InvalidTripStateException("Only completed trips can be reviewed");
        }
        if (trip.getDriver() == null) {
            throw new InvalidTripStateException("Trip has no assigned driver to review");
        }
        if (reviewRepository.findByTripId(tripId).isPresent()) {
            throw new DuplicateResourceException("A review already exists for this trip");
        }

        Review review = Review.builder()
                .trip(trip)
                .rider(trip.getRider())
                .driver(trip.getDriver())
                .rating(request.rating())
                .comment(request.comment())
                .build();

        review = reviewRepository.save(review);
        recomputeDriverRating(trip.getDriver());

        return ReviewResponse.builder()
                .id(review.getId())
                .tripId(trip.getId())
                .driverId(trip.getDriver().getId())
                .rating(review.getRating())
                .comment(review.getComment())
                .createdAt(review.getCreatedAt())
                .build();
    }

    private void recomputeDriverRating(DriverProfile driver) {
        List<Review> reviews = reviewRepository.findByDriverId(driver.getId());
        if (reviews.isEmpty()) {
            return;
        }
        double average = reviews.stream().mapToInt(Review::getRating).average().orElse(5.0);
        driver.setRating(BigDecimal.valueOf(average).setScale(2, RoundingMode.HALF_UP));
        driverProfileRepository.save(driver);
    }
}
