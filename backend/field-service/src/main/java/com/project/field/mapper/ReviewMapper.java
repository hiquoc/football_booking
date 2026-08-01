package com.project.field.mapper;

import com.project.field.dto.ReviewDto;
import com.project.field.dto.ReviewRequest;
import com.project.field.entity.Review;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class ReviewMapper {

    public ReviewDto toDto(Review entity) {
        return toDto(entity, null);
    }

    public ReviewDto toDto(Review entity, String fullName) {
        if (entity == null) return null;
        return ReviewDto.builder()
                .id(entity.getId())
                .fieldId(entity.getField() != null ? entity.getField().getId() : null)
                .userId(entity.getUserId())
                .fullName(fullName)
                .rating(entity.getRating())
                .comment(entity.getComment())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public Review toEntity(ReviewRequest request, UUID userId) {
        if (request == null) return null;
        return Review.builder()
                .userId(userId)
                .rating(request.getRating())
                .comment(request.getComment())
                .build();
    }
}
