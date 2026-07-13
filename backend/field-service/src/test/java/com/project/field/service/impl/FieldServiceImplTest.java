package com.project.field.service.impl;

import com.project.common.exception.ForbiddenException;
import com.project.common.security.UserPrincipal;
import com.project.field.dto.FieldDto;
import com.project.field.entity.Field;
import com.project.field.enums.FieldStatus;
import com.project.field.mapper.FieldMapper;
import com.project.field.repository.FieldCardQueryRepository;
import com.project.field.repository.FieldFavoriteRepository;
import com.project.field.repository.FieldImageRepository;
import com.project.field.repository.FieldOperatingHoursRepository;
import com.project.field.repository.FieldRepository;
import com.project.field.repository.SubFieldRepository;
import com.project.field.service.CloudinaryService;
import com.project.field.client.UserServiceClient;
import com.project.field.kafka.FieldEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FieldServiceImplTest {

    private static final UUID FIELD_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID OWNER_ID = UUID.fromString("b1e1c606-6b76-4154-af38-7dda890395ce");
    private static final UUID OTHER_USER_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    @Mock
    private FieldRepository fieldRepository;

    @Mock
    private FieldCardQueryRepository fieldCardQueryRepository;

    @Mock
    private FieldImageRepository fieldImageRepository;

    @Mock
    private FieldFavoriteRepository fieldFavoriteRepository;

    @Mock
    private FieldOperatingHoursRepository fieldOperatingHoursRepository;

    @Mock
    private SubFieldRepository subFieldRepository;

    @Mock
    private FieldMapper fieldMapper;

    @Mock
    private UserServiceClient userServiceClient;

    @Mock
    private CloudinaryService cloudinaryService;

    @Mock
    private FieldEventPublisher fieldEventPublisher;

    private FieldServiceImpl fieldService;

    @BeforeEach
    void setUp() {
        fieldService = new FieldServiceImpl(
                fieldRepository,
                fieldCardQueryRepository,
                fieldImageRepository,
                fieldFavoriteRepository,
                fieldOperatingHoursRepository,
                subFieldRepository,
                fieldMapper,
                userServiceClient,
                cloudinaryService,
                fieldEventPublisher);
    }

    @Test
    void getWithDetailsById_allowsAnonymousViewForApprovedField() {
        Field field = field(FieldStatus.APPROVED);
        when(fieldRepository.findWithDetailsById(FIELD_ID)).thenReturn(Optional.of(field));
        when(fieldImageRepository.findByFieldIdAndImageUrlIsNotNull(FIELD_ID)).thenReturn(List.of());
        when(subFieldRepository.findByFieldId(FIELD_ID)).thenReturn(List.of());
        when(fieldMapper.toDto(any(Field.class), org.mockito.ArgumentMatchers.eq(false))).thenReturn(FieldDto.builder().id(FIELD_ID).ownerId(OWNER_ID).status(FieldStatus.APPROVED).build());

        fieldService.getWithDetailsById(FIELD_ID, null);

        verify(fieldMapper).toDto(field, false);
    }

    @Test
    void getWithDetailsById_allowsOwnerToViewUnapprovedField() {
        Field field = field(FieldStatus.PENDING);
        when(fieldRepository.findWithDetailsById(FIELD_ID)).thenReturn(Optional.of(field));
        when(fieldImageRepository.findByFieldIdAndImageUrlIsNotNull(FIELD_ID)).thenReturn(List.of());
        when(subFieldRepository.findByFieldId(FIELD_ID)).thenReturn(List.of());
        when(fieldMapper.toDto(any(Field.class), org.mockito.ArgumentMatchers.eq(false))).thenReturn(FieldDto.builder().id(FIELD_ID).ownerId(OWNER_ID).status(FieldStatus.PENDING).build());

        fieldService.getWithDetailsById(FIELD_ID, new UserPrincipal(OWNER_ID, "owner@example.com", "OWNER"));

        verify(fieldMapper).toDto(field, false);
    }

    @Test
    void getWithDetailsById_allowsAdminToViewUnapprovedField() {
        Field field = field(FieldStatus.REJECTED);
        when(fieldRepository.findWithDetailsById(FIELD_ID)).thenReturn(Optional.of(field));
        when(fieldImageRepository.findByFieldIdAndImageUrlIsNotNull(FIELD_ID)).thenReturn(List.of());
        when(subFieldRepository.findByFieldId(FIELD_ID)).thenReturn(List.of());
        when(fieldMapper.toDto(any(Field.class), org.mockito.ArgumentMatchers.eq(false))).thenReturn(FieldDto.builder().id(FIELD_ID).ownerId(OWNER_ID).status(FieldStatus.REJECTED).build());

        fieldService.getWithDetailsById(FIELD_ID, new UserPrincipal(OTHER_USER_ID, "admin@example.com", "ADMIN"));

        verify(fieldMapper).toDto(field, false);
    }

    @Test
    void getWithDetailsById_rejectsAnonymousViewForUnapprovedField() {
        when(fieldRepository.findWithDetailsById(FIELD_ID)).thenReturn(Optional.of(field(FieldStatus.PENDING)));

        assertThatThrownBy(() -> fieldService.getWithDetailsById(FIELD_ID, null))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("You don't have permission to do this");

        verify(fieldImageRepository, never()).findByFieldIdAndImageUrlIsNotNull(FIELD_ID);
        verify(subFieldRepository, never()).findByFieldId(FIELD_ID);
    }

    @Test
    void getWithDetailsById_rejectsDifferentOwnerForUnapprovedField() {
        when(fieldRepository.findWithDetailsById(FIELD_ID)).thenReturn(Optional.of(field(FieldStatus.PENDING)));

        assertThatThrownBy(() -> fieldService.getWithDetailsById(
                FIELD_ID,
                new UserPrincipal(OTHER_USER_ID, "owner@example.com", "OWNER")))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("You don't have permission to do this");

        verify(fieldImageRepository, never()).findByFieldIdAndImageUrlIsNotNull(FIELD_ID);
        verify(subFieldRepository, never()).findByFieldId(FIELD_ID);
    }

    private Field field(FieldStatus status) {
        return Field.builder()
                .id(FIELD_ID)
                .ownerId(OWNER_ID)
                .name("ABC Football Center")
                .address("123 Nguyen Hue")
                .ward("Phuong Sai Gon")
                .wardCode("26743")
                .province("Thanh pho Ho Chi Minh")
                .provinceCode("79")
                .legacyWard("Phuong Ben Nghe")
                .legacyWardCode("26743")
                .legacyDistrict("Quan 1")
                .legacyProvince("Thanh pho Ho Chi Minh")
                .phoneNumber("0862470050")
                .status(status)
                .active(true)
                .build();
    }
}
