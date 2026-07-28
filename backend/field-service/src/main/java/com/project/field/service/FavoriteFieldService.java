package com.project.field.service;

import com.project.common.dto.PageResponse;
import com.project.field.dto.FavoriteCheckResponse;
import com.project.field.dto.FieldDto;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface FavoriteFieldService {
    PageResponse<FieldDto> getFavorites(UUID userId, Pageable pageable);
    FieldDto addFavorite(UUID userId, UUID fieldId);
    void removeFavorite(UUID userId, UUID fieldId);
    FavoriteCheckResponse checkFavorite(UUID userId, UUID fieldId);
}
