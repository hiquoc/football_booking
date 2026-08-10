package com.project.booking.community.service;

import com.project.booking.community.dto.*;
import com.project.common.dto.PageResponse;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface CommunityPostService {
    CommunityPostResponse create(UUID userId, CreateCommunityPostRequest request);
    CommunityPostResponse update(UUID userId, UUID postId, UpdateCommunityPostRequest request);
    CommunityPostResponse close(UUID userId, UUID postId);
    CommunityPostResponse markFull(UUID userId, UUID postId);
    CommunityPostResponse get(UUID viewerId, UUID postId);
    PageResponse<CommunityPostResponse> search(CommunityPostSearchRequest request, Pageable pageable);
    CommunityApplicationResponse apply(UUID userId, UUID postId, CommunityApplicationRequest request);
    CommunityApplicationResponse withdraw(UUID userId, UUID postId);
    CommunityApplicationResponse accept(UUID userId, UUID postId, UUID applicationId);
    CommunityApplicationResponse reject(UUID userId, UUID postId, UUID applicationId);
    java.util.List<MatchEvaluationResponse> getEvaluations(UUID userId, UUID postId);
    MatchEvaluationResponse evaluate(UUID userId, UUID postId, MatchEvaluationRequest request);
}
