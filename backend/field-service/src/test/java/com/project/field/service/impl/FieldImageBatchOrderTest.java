package com.project.field.service.impl;

import com.project.field.client.UserServiceClient;
import com.project.field.dto.FieldImageDto;
import com.project.field.dto.FieldImageOrderRequest;
import com.project.field.entity.Field;
import com.project.field.entity.FieldImage;
import com.project.field.exceptions.FieldNotFoundException;
import com.project.field.kafka.FieldEventPublisher;
import com.project.field.mapper.FieldMapper;
import com.project.field.mapper.FieldTypeMapper;
import com.project.field.mapper.SubFieldMapper;
import com.project.field.repository.FieldCardQueryRepository;
import com.project.field.repository.FieldFavoriteRepository;
import com.project.field.repository.FieldImageRepository;
import com.project.field.repository.FieldOperatingHoursRepository;
import com.project.field.repository.FieldRepository;
import com.project.field.repository.SubFieldRepository;
import com.project.field.service.CloudinaryService;
import org.junit.jupiter.api.Test;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FieldImageBatchOrderTest {

    @Test
    void failedDatabaseSaveUsesOneBatchCleanupCall() {
        UUID fieldId = UUID.randomUUID();
        List<String> uploadedUrls = List.of("first.jpg", "second.jpg");
        FieldRepository fieldRepository = mock(FieldRepository.class);
        CloudinaryService cloudinaryService = mock(CloudinaryService.class);
        when(cloudinaryService.uploadImages(anyList())).thenReturn(uploadedUrls);
        when(fieldRepository.findById(fieldId)).thenReturn(java.util.Optional.empty());
        FieldServiceImpl service = new FieldServiceImpl(
                fieldRepository,
                mock(FieldCardQueryRepository.class),
                mock(FieldImageRepository.class),
                mock(FieldFavoriteRepository.class),
                mock(FieldOperatingHoursRepository.class),
                mock(SubFieldRepository.class),
                new FieldMapper(mock(FieldTypeMapper.class), mock(SubFieldMapper.class)),
                mock(UserServiceClient.class),
                cloudinaryService,
                mock(FieldEventPublisher.class),
                new OperatingHoursPriceRuleSynchronizer());

        assertThatThrownBy(() -> service.uploadImages(fieldId, List.of(mock(MultipartFile.class))))
                .isInstanceOf(FieldNotFoundException.class);

        verify(cloudinaryService).deleteImages(uploadedUrls);
    }

    @Test
    void omittedOrdersAppendAfterExistingImages() {
        UUID fieldId = UUID.randomUUID();
        Field field = Field.builder()
                .id(fieldId)
                .images(new ArrayList<>(List.of(
                        FieldImage.builder().displayOrder(0).build(),
                        FieldImage.builder().displayOrder(2).build())))
                .build();
        FieldRepository fieldRepository = mock(FieldRepository.class);
        FieldImageRepository imageRepository = mock(FieldImageRepository.class);
        when(fieldRepository.findById(fieldId)).thenReturn(java.util.Optional.of(field));
        when(imageRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        FieldServiceImpl service = new FieldServiceImpl(
                fieldRepository,
                mock(FieldCardQueryRepository.class),
                imageRepository,
                mock(FieldFavoriteRepository.class),
                mock(FieldOperatingHoursRepository.class),
                mock(SubFieldRepository.class),
                new FieldMapper(mock(FieldTypeMapper.class), mock(SubFieldMapper.class)),
                mock(UserServiceClient.class),
                mock(CloudinaryService.class),
                mock(FieldEventPublisher.class),
                new OperatingHoursPriceRuleSynchronizer());

        List<FieldImageDto> result = service.addImages(
                fieldId,
                List.of("first.jpg", "second.jpg"));

        assertThat(result).extracting(FieldImageDto::getDisplayOrder).containsExactly(3, 4);
        assertThat(result).extracting(FieldImageDto::getIsPrimary).containsOnly(false);
    }

    @Test
    void orderingEndpointReordersImagesAndSelectsOnePrimaryImage() {
        UUID fieldId = UUID.randomUUID();
        FieldImage image10 = FieldImage.builder().id(10L).imageUrl("10.jpg").displayOrder(0).isPrimary(false).build();
        FieldImage image11 = FieldImage.builder().id(11L).imageUrl("11.jpg").displayOrder(1).isPrimary(true).build();
        FieldImage image12 = FieldImage.builder().id(12L).imageUrl("12.jpg").displayOrder(2).isPrimary(false).build();
        Field field = Field.builder()
                .id(fieldId)
                .images(new ArrayList<>(List.of(image10, image11, image12)))
                .build();
        FieldRepository fieldRepository = mock(FieldRepository.class);
        FieldImageRepository imageRepository = mock(FieldImageRepository.class);
        when(fieldRepository.findById(fieldId)).thenReturn(java.util.Optional.of(field));
        when(imageRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        FieldServiceImpl service = new FieldServiceImpl(
                fieldRepository,
                mock(FieldCardQueryRepository.class),
                imageRepository,
                mock(FieldFavoriteRepository.class),
                mock(FieldOperatingHoursRepository.class),
                mock(SubFieldRepository.class),
                new FieldMapper(mock(FieldTypeMapper.class), mock(SubFieldMapper.class)),
                mock(UserServiceClient.class),
                mock(CloudinaryService.class),
                mock(FieldEventPublisher.class),
                new OperatingHoursPriceRuleSynchronizer());

        List<FieldImageDto> result = service.updateImageOrder(
                fieldId,
                FieldImageOrderRequest.builder()
                        .imageIds(List.of(12L, 10L, 11L))
                        .build());

        assertThat(result).extracting(FieldImageDto::getId).containsExactly(12L, 10L, 11L);
        assertThat(result).extracting(FieldImageDto::getDisplayOrder).containsExactly(0, 1, 2);
        assertThat(result).filteredOn(FieldImageDto::getIsPrimary)
                .extracting(FieldImageDto::getId)
                .containsExactly(12L);
    }
}
