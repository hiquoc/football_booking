package com.project.field.service;

import com.project.common.security.UserPrincipal;
import com.project.field.dto.ReviewDto;
import com.project.field.dto.ReviewRequest;

import java.util.List;
import java.util.UUID;

public interface ReviewService {
    ReviewDto create(UserPrincipal user, ReviewRequest request);
    List<ReviewDto> getByFieldId(UUID fieldId);
}
