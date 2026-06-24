package com.project.field.service.impl;

import com.project.common.security.UserPrincipal;
import com.project.field.dto.ReviewDto;
import com.project.field.dto.ReviewRequest;
import com.project.field.entity.Field;
import com.project.field.entity.Review;
import com.project.field.mapper.ReviewMapper;
import com.project.field.repository.FieldRepository;
import com.project.field.repository.ReviewRepository;
import com.project.field.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import com.project.field.exceptions.FieldNotFoundException;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final FieldRepository fieldRepository;
    private final ReviewMapper reviewMapper;

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

        return reviewMapper.toDto(review);
    }

    @Override
    public List<ReviewDto> getByFieldId(UUID fieldId) {
        return reviewRepository.findByFieldId(fieldId).stream()
                .map(reviewMapper::toDto)
                .collect(Collectors.toList());
    }
}
