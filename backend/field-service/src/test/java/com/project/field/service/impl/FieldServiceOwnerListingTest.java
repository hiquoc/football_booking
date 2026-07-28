package com.project.field.service.impl;

import com.project.field.client.UserServiceClient;
import com.project.field.dto.FieldDto;
import com.project.field.entity.Field;
import com.project.field.kafka.FieldEventPublisher;
import com.project.field.mapper.FieldMapper;
import com.project.field.repository.FieldImageRepository;
import com.project.field.repository.FieldOperatingHoursRepository;
import com.project.field.repository.FieldRepository;
import com.project.field.service.CloudinaryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FieldServiceOwnerListingTest {

    @Mock
    private FieldRepository fieldRepository;
    @Mock
    private FieldImageRepository fieldImageRepository;
    @Mock
    private FieldOperatingHoursRepository fieldOperatingHoursRepository;
    @Mock
    private FieldMapper fieldMapper;
    @Mock
    private UserServiceClient userServiceClient;
    @Mock
    private CloudinaryService cloudinaryService;
    @Mock
    private FieldEventPublisher fieldEventPublisher;
    @Mock
    private OperatingHoursPriceRuleSynchronizer operatingHoursPriceRuleSynchronizer;
    @InjectMocks
    private FieldServiceImpl fieldService;

    @Test
    void getByOwnerIdScopesTheRepositoryQueryAndMapsThePage() {
        UUID ownerId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(1, 5);
        Field field = new Field();
        FieldDto dto = FieldDto.builder().id(UUID.randomUUID()).ownerId(ownerId).build();

        when(fieldRepository.findByOwnerId(ownerId, pageable))
                .thenReturn(new PageImpl<>(List.of(field), pageable, 6));
        when(fieldMapper.toDto(field)).thenReturn(dto);

        var result = fieldService.getByOwnerId(ownerId, pageable);

        assertThat(result.getContent()).containsExactly(dto);
        assertThat(result.getPage()).isEqualTo(1);
        assertThat(result.getSize()).isEqualTo(5);
        assertThat(result.getTotalElements()).isEqualTo(6);
        verify(fieldRepository).findByOwnerId(ownerId, pageable);
    }
}
