package com.project.field.service.impl;

import com.project.common.cache.CacheNames;
import com.project.common.dto.PageResponse;
import com.project.field.dto.FavoriteCheckResponse;
import com.project.field.dto.FieldDto;
import com.project.field.entity.Field;
import com.project.field.entity.FieldFavorite;
import com.project.field.exceptions.FieldNotFoundException;
import com.project.field.mapper.FieldMapper;
import com.project.field.repository.FieldFavoriteRepository;
import com.project.field.repository.FieldRepository;
import com.project.field.service.FavoriteFieldService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FavoriteFieldServiceImpl implements FavoriteFieldService {

    private final FieldFavoriteRepository favoriteRepository;
    private final FieldRepository fieldRepository;
    private final FieldMapper fieldMapper;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<FieldDto> getFavorites(UUID userId, Pageable pageable) {
        return PageResponse.from(favoriteRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(FieldFavorite::getField)
                .map(field -> fieldMapper.toDto(field, true)));
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = {CacheNames.FIELD_DETAIL, CacheNames.FIELD_SEARCH}, allEntries = true)
    public FieldDto addFavorite(UUID userId, UUID fieldId) {
        Field field = fieldRepository.findById(fieldId)
                .orElseThrow(() -> new FieldNotFoundException(fieldId));
        if (!favoriteRepository.existsByUserIdAndFieldId(userId, fieldId)) {
            try {
                favoriteRepository.save(FieldFavorite.builder()
                        .userId(userId)
                        .field(field)
                        .build());
            } catch (DataIntegrityViolationException ignored) {
                // Concurrent duplicate favorite creation is still an idempotent success.
            }
        }
        return fieldMapper.toDto(field, true);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = {CacheNames.FIELD_DETAIL, CacheNames.FIELD_SEARCH}, allEntries = true)
    public void removeFavorite(UUID userId, UUID fieldId) {
        favoriteRepository.findByUserIdAndFieldId(userId, fieldId)
                .ifPresent(favoriteRepository::delete);
    }

    @Override
    @Transactional(readOnly = true)
    public FavoriteCheckResponse checkFavorite(UUID userId, UUID fieldId) {
        return new FavoriteCheckResponse(favoriteRepository.existsByUserIdAndFieldId(userId, fieldId));
    }
}
