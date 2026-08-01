package com.project.field.service;

import com.project.common.dto.PageResponse;
import com.project.common.security.UserPrincipal;
import com.project.field.dto.ReviewDto;
import com.project.field.dto.ReviewRequest;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ReviewService {
    ReviewDto create(UserPrincipal user, ReviewRequest request);
    PageResponse<ReviewDto> getByFieldId(UUID fieldId, Pageable pageable);
}
