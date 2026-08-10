package com.project.field.service.impl;

import com.project.common.security.UserPrincipal;
import com.project.common.cache.CacheNames;
import com.project.common.dto.PageResponse;
import com.project.common.exception.BadRequestException;
import com.project.field.dto.ReviewDto;
import com.project.field.dto.ReviewRequest;
import com.project.field.dto.UserDto;
import com.project.field.entity.Field;
import com.project.field.entity.Review;
import com.project.field.client.BookingServiceClient;
import com.project.field.client.UserServiceClient;
import com.project.field.mapper.ReviewMapper;
import com.project.field.repository.FieldRepository;
import com.project.field.repository.ReviewRepository;
import com.project.field.service.ReviewService;
import com.project.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.util.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;
import com.project.field.exceptions.FieldNotFoundException;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final FieldRepository fieldRepository;
    private final ReviewMapper reviewMapper;
    private final UserServiceClient userServiceClient;
    private final BookingServiceClient bookingServiceClient;

    @Override
    @Transactional
    @CacheEvict(cacheNames = {CacheNames.FIELD_DETAIL, CacheNames.FIELD_SEARCH}, allEntries = true)
    public ReviewMutationResult create(UserPrincipal user, ReviewRequest request) {
        Field field = fieldRepository.findById(request.getFieldId())
                .orElseThrow(() -> new FieldNotFoundException(request.getFieldId()));

        ensureCompletedBooking(user.id(), field.getId());

        Review review = reviewRepository.findFirstByFieldIdAndUserIdAndDeletedFalseOrderByUpdatedAtDesc(field.getId(), user.id())
                .orElse(null);
        boolean created = review == null;
        Integer previousRating = created ? null : review.getRating();
        if (created) {
            review = reviewMapper.toEntity(request, user.id());
            review.setField(field);
        } else {
            review.setRating(request.getRating());
            review.setComment(request.getComment());
        }
        review = reviewRepository.save(review);

        updateFieldRating(field, previousRating, request.getRating(), created);

        fieldRepository.save(field);

        return new ReviewMutationResult(reviewMapper.toDto(review, reviewDisplayName(user.id())), created);
    }

    @Override
    public PageResponse<ReviewDto> getByFieldId(UUID fieldId, Pageable pageable) {
        Page<ReviewDto> reviews = reviewRepository.findByFieldIdOrderByCreatedAtDesc(fieldId, pageable)
                .map(review -> reviewMapper.toDto(review, reviewDisplayName(review.getUserId())));
        return PageResponse.from(reviews);
    }

    @Override
    public ReviewDto getCurrentUserReview(UserPrincipal user, UUID fieldId) {
        return reviewRepository.findFirstByFieldIdAndUserIdAndDeletedFalseOrderByUpdatedAtDesc(fieldId, user.id())
                .map(review -> reviewMapper.toDto(review, reviewDisplayName(user.id())))
                .orElse(null);
    }

    private void ensureCompletedBooking(UUID userId, UUID fieldId) {
        ApiResponse<Boolean> response = bookingServiceClient.hasCompletedBookingAtField(userId, fieldId);
        if (response == null || !Boolean.TRUE.equals(response.getData())) {
            throw new BadRequestException(
                    "A completed booking at this field is required before reviewing.",
                    "REVIEW_COMPLETED_BOOKING_REQUIRED");
        }
    }

    private void updateFieldRating(Field field, Integer previousRating, Integer newRating, boolean created) {
        int currentTotal = field.getTotalReviews() != null ? field.getTotalReviews() : 0;
        BigDecimal currentAverage = field.getRatingAverage() != null ? field.getRatingAverage() : BigDecimal.ZERO;
        int newTotal = created ? currentTotal + 1 : currentTotal;
        BigDecimal sum = currentAverage.multiply(BigDecimal.valueOf(currentTotal))
                .add(BigDecimal.valueOf(newRating));
        if (!created && previousRating != null) {
            sum = sum.subtract(BigDecimal.valueOf(previousRating));
        }

        BigDecimal newAverage = newTotal > 0
                ? sum.divide(BigDecimal.valueOf(newTotal), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        field.setTotalReviews(newTotal);
        field.setRatingAverage(newAverage);
    }

    private String reviewDisplayName(UUID userId) {
        try {
            ApiResponse<UserDto> response = userServiceClient.getUserProfile(userId);
            UserDto user = response != null ? response.getData() : null;
            if (user != null && StringUtils.hasText(user.getFullName())) {
                return user.getFullName().trim();
            }
            return fallbackDisplayName(user != null ? user.getPhoneNumber() : null);
        } catch (RuntimeException ex) {
            return "Người dùng PitchUp";
        }
    }

    private String fallbackDisplayName(String phoneNumber) {
        if (!StringUtils.hasText(phoneNumber)) {
            return "Người dùng PitchUp";
        }
        String digits = phoneNumber.replaceAll("\\D", "");
        if (digits.length() >= 4) {
            return "User " + digits.substring(digits.length() - 4);
        }
        if (digits.length() >= 3) {
            return "User " + digits.substring(digits.length() - 3);
        }
        return "Người dùng PitchUp";
    }
}
