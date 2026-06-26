package com.project.field.service;

import com.project.common.dto.PageResponse;
import com.project.field.dto.FieldDto;
import com.project.field.dto.FieldImageDto;
import com.project.field.dto.FieldImageOrderRequest;
import com.project.field.dto.FieldRequest;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

public interface FieldService {
    FieldDto create(UUID ownerId,FieldRequest request);
    FieldDto update(UUID id,UUID ownerId, FieldRequest request);
    FieldDto getById(UUID id);
    PageResponse<FieldDto> getAll(Pageable pageable);
    List<FieldImageDto> addImages(UUID fieldId, List<String> imageUrls);
    List<FieldImageDto> uploadImages(UUID fieldId, List<MultipartFile> files);
    List<FieldImageDto> updateImageOrder(UUID fieldId, FieldImageOrderRequest request);
    void deleteImage(Long imageId);
}
