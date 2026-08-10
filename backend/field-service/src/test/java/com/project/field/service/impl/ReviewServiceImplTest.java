package com.project.field.service.impl;

import com.project.common.dto.ApiResponse;
import com.project.common.exception.BadRequestException;
import com.project.common.security.UserPrincipal;
import com.project.field.client.BookingServiceClient;
import com.project.field.client.UserServiceClient;
import com.project.field.dto.ReviewDto;
import com.project.field.dto.ReviewRequest;
import com.project.field.entity.Field;
import com.project.field.entity.Review;
import com.project.field.mapper.ReviewMapper;
import com.project.field.repository.FieldRepository;
import com.project.field.repository.ReviewRepository;
import com.project.field.service.ReviewService.ReviewMutationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewServiceImplTest {

    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID FIELD_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID REVIEW_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private FieldRepository fieldRepository;

    @Mock
    private ReviewMapper reviewMapper;

    @Mock
    private UserServiceClient userServiceClient;

    @Mock
    private BookingServiceClient bookingServiceClient;

    private ReviewServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ReviewServiceImpl(reviewRepository, fieldRepository, reviewMapper, userServiceClient, bookingServiceClient);
    }

    @Test
    void createInsertsReviewWhenUserHasCompletedBooking() {
        Field field = field(BigDecimal.ZERO, 0);
        Review review = review(5);
        ReviewDto dto = ReviewDto.builder().id(REVIEW_ID).fieldId(FIELD_ID).userId(USER_ID).rating(5).build();
        ReviewRequest request = ReviewRequest.builder().fieldId(FIELD_ID).rating(5).comment("Great").build();
        when(fieldRepository.findById(FIELD_ID)).thenReturn(Optional.of(field));
        when(bookingServiceClient.hasCompletedBookingAtField(USER_ID, FIELD_ID)).thenReturn(ApiResponse.success(true));
        when(reviewRepository.findFirstByFieldIdAndUserIdAndDeletedFalseOrderByUpdatedAtDesc(FIELD_ID, USER_ID)).thenReturn(Optional.empty());
        when(reviewMapper.toEntity(request, USER_ID)).thenReturn(review);
        when(reviewRepository.save(review)).thenReturn(review);
        when(reviewMapper.toDto(review, "Người dùng PitchUp")).thenReturn(dto);

        ReviewMutationResult result = service.create(user(), request);

        assertThat(result.created()).isTrue();
        assertThat(result.review()).isSameAs(dto);
        assertThat(field.getTotalReviews()).isEqualTo(1);
        assertThat(field.getRatingAverage()).isEqualByComparingTo("5.00");
    }

    @Test
    void createUpdatesExistingReviewAndKeepsTotalReviewCount() {
        Field field = field(new BigDecimal("4.00"), 2);
        Review review = review(3);
        ReviewDto dto = ReviewDto.builder().id(REVIEW_ID).fieldId(FIELD_ID).userId(USER_ID).rating(5).build();
        ReviewRequest request = ReviewRequest.builder().fieldId(FIELD_ID).rating(5).comment("Better").build();
        when(fieldRepository.findById(FIELD_ID)).thenReturn(Optional.of(field));
        when(bookingServiceClient.hasCompletedBookingAtField(USER_ID, FIELD_ID)).thenReturn(ApiResponse.success(true));
        when(reviewRepository.findFirstByFieldIdAndUserIdAndDeletedFalseOrderByUpdatedAtDesc(FIELD_ID, USER_ID)).thenReturn(Optional.of(review));
        when(reviewRepository.save(review)).thenReturn(review);
        when(reviewMapper.toDto(review, "Người dùng PitchUp")).thenReturn(dto);

        ReviewMutationResult result = service.create(user(), request);

        assertThat(result.created()).isFalse();
        assertThat(review.getRating()).isEqualTo(5);
        assertThat(review.getComment()).isEqualTo("Better");
        assertThat(field.getTotalReviews()).isEqualTo(2);
        assertThat(field.getRatingAverage()).isEqualByComparingTo("5.00");
    }

    @Test
    void createRejectsReviewWithoutCompletedBooking() {
        Field field = field(BigDecimal.ZERO, 0);
        ReviewRequest request = ReviewRequest.builder().fieldId(FIELD_ID).rating(5).build();
        when(fieldRepository.findById(FIELD_ID)).thenReturn(Optional.of(field));
        when(bookingServiceClient.hasCompletedBookingAtField(USER_ID, FIELD_ID)).thenReturn(ApiResponse.success(false));

        assertThatThrownBy(() -> service.create(user(), request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("A completed booking at this field is required before reviewing.");
        verify(reviewRepository, never()).save(any(Review.class));
    }

    private UserPrincipal user() {
        return new UserPrincipal(USER_ID, "client@example.com", "CLIENT");
    }

    private Field field(BigDecimal ratingAverage, int totalReviews) {
        return Field.builder()
                .id(FIELD_ID)
                .ownerId(UUID.randomUUID())
                .name("Field")
                .address("Address")
                .ward("Ward")
                .wardCode("1")
                .province("Province")
                .provinceCode("2")
                .legacyWard("Ward")
                .legacyWardCode("1")
                .legacyDistrict("District")
                .legacyProvince("Province")
                .latitude(BigDecimal.ZERO)
                .longitude(BigDecimal.ZERO)
                .phoneNumber("0900000000")
                .ratingAverage(ratingAverage)
                .totalReviews(totalReviews)
                .build();
    }

    private Review review(int rating) {
        return Review.builder()
                .id(REVIEW_ID)
                .field(field(BigDecimal.ZERO, 0))
                .userId(USER_ID)
                .rating(rating)
                .build();
    }
}
