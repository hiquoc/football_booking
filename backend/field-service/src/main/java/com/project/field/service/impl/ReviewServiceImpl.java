package com.project.field.service.impl;

import com.project.common.security.UserPrincipal;
import com.project.common.dto.PageResponse;
import com.project.field.dto.ReviewDto;
import com.project.field.dto.ReviewRequest;
import com.project.field.dto.UserDto;
import com.project.field.entity.Field;
import com.project.field.entity.Review;
import com.project.field.client.UserServiceClient;
import com.project.field.mapper.ReviewMapper;
import com.project.field.repository.FieldRepository;
import com.project.field.repository.ReviewRepository;
import com.project.field.service.ReviewService;
import com.project.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
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

    @Override
    @Transactional
    public ReviewDto create(UserPrincipal user, ReviewRequest request) {
        Field field = fieldRepository.findById(request.getFieldId())
                .orElseThrow(() -> new FieldNotFoundException(request.getFieldId()));

        Review review = reviewMapper.toEntity(request, user.id());
        review.setField(field);
        review = reviewRepository.save(review);

        Integer newTotal = field.getTotalReviews() + 1;
        BigDecimal sum = field.getRatingAverage()
                .multiply(BigDecimal.valueOf(field.getTotalReviews()))
                .add(BigDecimal.valueOf(request.getRating()));
        BigDecimal newAverage = sum.divide(BigDecimal.valueOf(newTotal), 2, RoundingMode.HALF_UP);

        field.setTotalReviews(newTotal);
        field.setRatingAverage(newAverage);
        fieldRepository.save(field);

        return reviewMapper.toDto(review, reviewDisplayName(user.id()));
    }

    @Override
    public PageResponse<ReviewDto> getByFieldId(UUID fieldId, Pageable pageable) {
        Page<ReviewDto> reviews = reviewRepository.findByFieldIdOrderByCreatedAtDesc(fieldId, pageable)
                .map(review -> reviewMapper.toDto(review, reviewDisplayName(review.getUserId())));
        return PageResponse.from(reviews);
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
