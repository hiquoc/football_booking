package com.project.field.service.impl;

import com.project.field.dto.FieldDto;
import com.project.field.entity.Field;
import com.project.field.entity.FieldFavorite;
import com.project.field.mapper.FieldMapper;
import com.project.field.repository.FieldFavoriteRepository;
import com.project.field.repository.FieldRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FavoriteFieldServiceImplTest {

    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID FIELD_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Mock
    private FieldFavoriteRepository favoriteRepository;

    @Mock
    private FieldRepository fieldRepository;

    @Mock
    private FieldMapper fieldMapper;

    private FavoriteFieldServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new FavoriteFieldServiceImpl(favoriteRepository, fieldRepository, fieldMapper);
    }

    @Test
    void addFavoriteIsIdempotentWhenFavoriteAlreadyExists() {
        Field field = field();
        FieldDto dto = FieldDto.builder().id(FIELD_ID).isFavorite(true).build();
        when(fieldRepository.findById(FIELD_ID)).thenReturn(Optional.of(field));
        when(favoriteRepository.existsByUserIdAndFieldId(USER_ID, FIELD_ID)).thenReturn(true);
        when(fieldMapper.toDto(field, true)).thenReturn(dto);

        FieldDto result = service.addFavorite(USER_ID, FIELD_ID);

        assertThat(result).isSameAs(dto);
        verify(favoriteRepository, never()).save(any(FieldFavorite.class));
    }

    @Test
    void addFavoriteTreatsConcurrentDuplicateAsSuccess() {
        Field field = field();
        FieldDto dto = FieldDto.builder().id(FIELD_ID).isFavorite(true).build();
        when(fieldRepository.findById(FIELD_ID)).thenReturn(Optional.of(field));
        when(favoriteRepository.existsByUserIdAndFieldId(USER_ID, FIELD_ID)).thenReturn(false);
        when(favoriteRepository.save(any(FieldFavorite.class))).thenThrow(new DataIntegrityViolationException("duplicate"));
        when(fieldMapper.toDto(field, true)).thenReturn(dto);

        FieldDto result = service.addFavorite(USER_ID, FIELD_ID);

        assertThat(result).isSameAs(dto);
    }

    @Test
    void removeFavoriteIsIdempotentWhenFavoriteDoesNotExist() {
        when(favoriteRepository.findByUserIdAndFieldId(USER_ID, FIELD_ID)).thenReturn(Optional.empty());

        service.removeFavorite(USER_ID, FIELD_ID);

        verify(favoriteRepository, never()).delete(any(FieldFavorite.class));
    }

    private Field field() {
        return Field.builder()
                .id(FIELD_ID)
                .name("Favorite Field")
                .build();
    }
}
