package com.project.field.service;

import com.project.common.dto.PageResponse;
import com.project.common.security.UserPrincipal;
import com.project.field.dto.FieldDto;
import com.project.field.dto.FieldCardDto;
import com.project.field.dto.FieldImageDto;
import com.project.field.dto.FieldImageOrderRequest;
import com.project.field.dto.FieldRequest;
import com.project.field.dto.FieldSearchOptionResponse;
import com.project.field.enums.FieldStatus;

import java.util.List;
import java.util.UUID;
import java.math.BigDecimal;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

public interface FieldService {
    FieldDto create(UUID ownerId,FieldRequest request);
    FieldDto update(UUID id,UUID ownerId, FieldRequest request);
    FieldDto getById(UUID id);
    FieldDto getWithDetailsById(UUID id, UserPrincipal currentUser);
    PageResponse<FieldDto> getByOwnerId(UUID ownerId, Pageable pageable);
    PageResponse<FieldDto> getAll(FieldStatus status, Pageable pageable, UserPrincipal currentUser);
    FieldDto updateStatus(UUID id, FieldStatus status);
    PageResponse<FieldCardDto> searchCards(String keyword, String fieldType, String subFieldType, String district, String provinceCode,
            BigDecimal latitude, BigDecimal longitude, Double radiusKm, String sortBy, String direction,
            int page, int size, UserPrincipal currentUser);
    List<FieldSearchOptionResponse> searchFieldOptions(String keyword);
    List<FieldImageDto> addImages(UUID fieldId, List<String> imageUrls);
    List<FieldImageDto> uploadImages(UUID fieldId, List<MultipartFile> files);
    List<FieldImageDto> updateImageOrder(UUID fieldId, FieldImageOrderRequest request);
    void deleteImage(Long imageId);
}
