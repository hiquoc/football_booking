package com.project.field.service;

import com.project.field.dto.FavoriteCheckResponse;
import com.project.field.dto.FieldDto;

import java.util.List;
import java.util.UUID;

public interface FavoriteFieldService {
    List<FieldDto> getFavorites(UUID userId);
    FieldDto addFavorite(UUID userId, UUID fieldId);
    void removeFavorite(UUID userId, UUID fieldId);
    FavoriteCheckResponse checkFavorite(UUID userId, UUID fieldId);
}
