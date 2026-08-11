package com.project.field.service.impl;

import com.project.common.exception.BadRequestException;
import com.project.field.dto.ImageUploadBatchConfirmRequest;
import com.project.field.dto.ImageUploadConfirmRequest;
import com.project.field.entity.Field;
import com.project.field.entity.FieldImage;
import com.project.field.mapper.FieldMapper;
import com.project.field.repository.FieldImageRepository;
import com.project.field.repository.FieldRepository;
import com.project.field.service.CloudinaryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FieldImageUploadServiceImplTest {

    private static final UUID FIELD_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID OWNER_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

    @Mock
    private FieldRepository fieldRepository;

    @Mock
    private FieldImageRepository imageRepository;

    @Mock
    private CloudinaryService cloudinary;

    @Mock
    private FieldMapper mapper;

    @Mock
    private PlatformTransactionManager transactionManager;

    private FieldImageUploadServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new FieldImageUploadServiceImpl(
                fieldRepository,
                imageRepository,
                cloudinary,
                mapper,
                transactionManager);
        ReflectionTestUtils.setField(service, "maxImageBytes", 10L * 1024L * 1024L);
    }

    @Test
    void confirmBatchRejectsImagesOverConfiguredSizeLimit() {
        String publicId = "field-image-public-id";
        Field field = Field.builder().id(FIELD_ID).ownerId(OWNER_ID).build();
        FieldImage image = FieldImage.builder()
                .field(field)
                .publicId(publicId)
                .uploadOwnerId(OWNER_ID)
                .build();
        when(imageRepository.findByFieldIdAndPublicId(FIELD_ID, publicId)).thenReturn(Optional.of(image));

        ImageUploadBatchConfirmRequest request = new ImageUploadBatchConfirmRequest(List.of(
                new ImageUploadConfirmRequest(
                        publicId,
                        "https://res.cloudinary.com/demo/image/upload/v1/field-image-public-id.webp",
                        1L,
                        "signature",
                        "webp",
                        1200,
                        800,
                        10L * 1024L * 1024L + 1L)));

        assertThatThrownBy(() -> service.confirmBatch(FIELD_ID, OWNER_ID, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("10 MB");
        verify(cloudinary, never()).verifyUploadResult(publicId, 1L, "signature");
    }
}
