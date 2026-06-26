package com.project.field.service.impl;

import com.project.common.dto.ApiResponse;
import com.project.common.dto.PageResponse;
import com.project.common.enums.UserType;
import com.project.common.exception.BadRequestException;
import com.project.common.exception.ForbiddenException;
import com.project.common.exception.NotFoundException;
import com.project.field.client.UserServiceClient;
import com.project.field.dto.FieldDto;
import com.project.field.dto.FieldImageDto;
import com.project.field.dto.FieldImageOrderRequest;
import com.project.field.dto.FieldRequest;
import com.project.field.dto.OperatingHoursRequest;
import com.project.field.dto.UserDto;
import com.project.field.entity.Field;
import com.project.field.entity.FieldImage;
import com.project.field.entity.FieldOperatingHours;
import com.project.field.exceptions.FieldNotFoundException;
import com.project.field.kafka.FieldEventPublisher;
import com.project.field.mapper.FieldMapper;
import com.project.field.repository.FieldImageRepository;
import com.project.field.repository.FieldOperatingHoursRepository;
import com.project.field.repository.FieldRepository;
import com.project.field.service.CloudinaryService;
import com.project.field.service.FieldService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.time.DayOfWeek;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FieldServiceImpl implements FieldService {

    private final FieldRepository fieldRepository;
    private final FieldImageRepository fieldImageRepository;
    private final FieldOperatingHoursRepository fieldOperatingHoursRepository;
    private final FieldMapper fieldMapper;
    private final UserServiceClient userServiceClient;
    private final CloudinaryService cloudinaryService;
    private final FieldEventPublisher fieldEventPublisher;

    @Override
    @Transactional
    public FieldDto create(UUID ownerId, FieldRequest request) {
        validateCreateRequest(request);
        validate(ownerId);

        Field field = fieldMapper.toEntity(ownerId, request);

        Field saved = fieldRepository.save(field);
        fieldEventPublisher.publishFieldOperatingHoursUpdated(createOperatingHours(saved, request));
        return fieldMapper.toDto(saved);
    }

    @Override
    @Transactional
    public FieldDto update(UUID id,UUID ownerId, FieldRequest request) {
        Field field = fieldRepository.findById(id)
                .orElseThrow(() -> new FieldNotFoundException(id));
        if(!field.getOwnerId().equals(ownerId)){
            throw new ForbiddenException("You don't have permission to do this");
        }
        validateUpdateRequest(request, field);

        // Use mapper to update only non-null fields
        fieldMapper.updateEntity(field, request);

        Field saved = fieldRepository.save(field);
        if (request.getOperatingHours() != null) {
            fieldEventPublisher.publishFieldOperatingHoursUpdated(upsertOperatingHours(saved, request));
        }
        return fieldMapper.toDto(saved);
    }

    @Override
    public FieldDto getById(UUID id) {
        return fieldRepository.findById(id)
                .map(fieldMapper::toDto)
                .orElseThrow(() -> new FieldNotFoundException(id));
    }

    @Override
    public PageResponse<FieldDto> getAll(Pageable pageable) {
        List<Field> Fields = fieldRepository.findAll();
        System.out.println(Fields);
        return PageResponse.from(fieldRepository.findAll(pageable).map(fieldMapper::toDto));
    }

    @Override
    @Transactional
    public List<FieldImageDto> addImages(
            UUID fieldId,
            List<String> imageUrls) {
        Field field = fieldRepository.findById(fieldId)
                .orElseThrow(() -> new FieldNotFoundException(fieldId));
        if (imageUrls == null || imageUrls.isEmpty()) {
            throw new BadRequestException("At least one image URL is required");
        }

        int nextDisplayOrder = nextDisplayOrder(field);
        List<FieldImage> images = new ArrayList<>();
        for (int index = 0; index < imageUrls.size(); index++) {
            images.add(FieldImage.builder()
                    .field(field)
                    .imageUrl(imageUrls.get(index))
                    .isPrimary(false)
                    .displayOrder(nextDisplayOrder + index)
                    .build());
        }

        return fieldImageRepository.saveAll(images).stream()
                .sorted(Comparator.comparing(FieldImage::getDisplayOrder).thenComparing(FieldImage::getId))
                .map(fieldMapper::toImageDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public List<FieldImageDto> uploadImages(UUID fieldId, List<MultipartFile> files) {
        List<String> imageUrls = cloudinaryService.uploadImages(files);
        try {
            return addImages(fieldId, imageUrls);
        } catch (RuntimeException ex) {
            imageUrls.forEach(cloudinaryService::deleteImage);
            throw ex;
        }
    }

    @Override
    @Transactional
    public void deleteImage(Long imageId) {
        fieldImageRepository.deleteById(imageId);
    }

    @Override
    @Transactional
    public List<FieldImageDto> updateImageOrder(UUID fieldId, FieldImageOrderRequest request) {
        Field field = fieldRepository.findById(fieldId)
                .orElseThrow(() -> new FieldNotFoundException(fieldId));
        List<FieldImage> existingImages = field.getImages() != null ? field.getImages() : List.of();
        List<Long> requestedIds = request.getImageIds();
        Set<Long> requestedIdSet = new HashSet<>(requestedIds);
        Set<Long> existingIdSet = existingImages.stream()
                .map(FieldImage::getId)
                .collect(Collectors.toSet());

        if (requestedIdSet.size() != requestedIds.size()) {
            throw new BadRequestException("Image IDs must not contain duplicates");
        }
        if (!requestedIdSet.equals(existingIdSet)) {
            throw new BadRequestException("Image IDs must include every image belonging to the field exactly once");
        }

        Map<Long, FieldImage> imagesById = existingImages.stream()
                .collect(Collectors.toMap(FieldImage::getId, image -> image));
        List<FieldImage> orderedImages = new ArrayList<>();
        for (int index = 0; index < requestedIds.size(); index++) {
            FieldImage image = imagesById.get(requestedIds.get(index));
            image.setDisplayOrder(index);
            image.setIsPrimary(image.getId().equals(request.getImageIds().getFirst()));
            orderedImages.add(image);
        }

        return fieldImageRepository.saveAll(orderedImages).stream()
                .sorted(Comparator.comparing(FieldImage::getDisplayOrder))
                .map(fieldMapper::toImageDto)
                .collect(Collectors.toList());
    }

    private int nextDisplayOrder(Field field) {
        return field.getImages() == null ? 0 : field.getImages().stream()
                .map(FieldImage::getDisplayOrder)
                .filter(Objects::nonNull)
                .max(Integer::compareTo)
                .map(order -> order + 1)
                .orElse(0);
    }

    private List<FieldOperatingHours> upsertOperatingHours(Field field, FieldRequest request) {
        return upsertOperatingHoursFromRequest(field, request.getOperatingHours());
    }

    private List<FieldOperatingHours> createOperatingHours(Field field, FieldRequest request) {
        List<FieldOperatingHours> hours = request.getOperatingHours().stream()
                .map(requestHours -> buildOperatingHours(field.getId(), requestHours))
                .toList();
        return fieldOperatingHoursRepository.saveAll(hours);
    }

    private List<FieldOperatingHours> upsertOperatingHoursFromRequest(Field field, List<OperatingHoursRequest> requests) {
        Map<DayOfWeek, FieldOperatingHours> existingByDay = fieldOperatingHoursRepository.findByFieldId(field.getId())
                .stream()
                .collect(Collectors.toMap(FieldOperatingHours::getDayOfWeek, hours -> hours));

        List<FieldOperatingHours> hours = requests.stream()
                .map(request -> {
                    FieldOperatingHours operatingHours = existingByDay.getOrDefault(
                            request.getDayOfWeek(),
                            FieldOperatingHours.builder()
                                    .fieldId(field.getId())
                                    .dayOfWeek(request.getDayOfWeek())
                                    .build());
                    operatingHours.setOpenTime(Boolean.TRUE.equals(request.getClosed()) ? null : request.getOpenTime());
                    operatingHours.setCloseTime(Boolean.TRUE.equals(request.getClosed()) ? null : request.getCloseTime());
                    operatingHours.setClosed(Boolean.TRUE.equals(request.getClosed()));
                    return operatingHours;
                })
                .toList();
        return fieldOperatingHoursRepository.saveAll(hours);
    }

    private FieldOperatingHours buildOperatingHours(UUID fieldId, OperatingHoursRequest request) {
        FieldOperatingHours operatingHours = FieldOperatingHours.builder()
                .fieldId(fieldId)
                .dayOfWeek(request.getDayOfWeek())
                .build();
        operatingHours.setOpenTime(Boolean.TRUE.equals(request.getClosed()) ? null : request.getOpenTime());
        operatingHours.setCloseTime(Boolean.TRUE.equals(request.getClosed()) ? null : request.getCloseTime());
        operatingHours.setClosed(Boolean.TRUE.equals(request.getClosed()));
        return operatingHours;
    }

    // ─── Private helpers ────────────────────────────────────────────────────────

    private void validate(UUID ownerId) {
        ApiResponse<UserDto> response = userServiceClient.getUserProfile(ownerId);
        if (response == null || !response.isSuccess() || response.getData() == null) {
            throw new NotFoundException("User not found with id " + ownerId);
        }
        if (response.getData().getUserType() != UserType.OWNER) {
            throw new ForbiddenException("You don't have permission to do this operation");
        }
    }

    private void validateCreateRequest(FieldRequest request) {
        if (request.getName() == null || request.getName().isBlank()) {
            throw new BadRequestException("Field name is required");
        }
        if (request.getAddress() == null || request.getAddress().isBlank()) {
            throw new BadRequestException("Field address is required");
        }
        if (request.getPhoneNumber() == null || request.getPhoneNumber().isBlank()) {
            throw new BadRequestException("Phone number is required");
        }
        validateOperatingHours(request, true);
    }

    private void validateUpdateRequest(FieldRequest request, Field field) {
        if (request.getName() != null && request.getName().isBlank()) {
            throw new BadRequestException("Field name must not be blank");
        }
        if (request.getAddress() != null && request.getAddress().isBlank()) {
            throw new BadRequestException("Field address must not be blank");
        }
        if (request.getPhoneNumber() != null && request.getPhoneNumber().isBlank()) {
            throw new BadRequestException("Phone number must not be blank");
        }
        validateOperatingHours(request, false);
    }

    private void validateOperatingHours(FieldRequest request, boolean create) {
        if (request.getOperatingHours() == null) {
            if (create) {
                throw new BadRequestException("Operating hours are required");
            }
            return;
        }
        validateCompleteWeek(request.getOperatingHours());
        request.getOperatingHours().forEach(this::validateOperatingHours);
    }

    private void validateCompleteWeek(List<OperatingHoursRequest> requests) {
        if (requests == null || requests.size() != DayOfWeek.values().length) {
            throw new BadRequestException("Operating hours must include exactly one record for each day of week");
        }
        if (requests.stream().anyMatch(request -> request.getDayOfWeek() == null)) {
            throw new BadRequestException("Day of week is required");
        }
        Set<DayOfWeek> days = requests.stream()
                .map(OperatingHoursRequest::getDayOfWeek)
                .collect(Collectors.toSet());
        if (days.size() != DayOfWeek.values().length) {
            throw new BadRequestException("Operating hours must include every day of week exactly once");
        }
    }

    private void validateOperatingHours(OperatingHoursRequest request) {
        if (Boolean.TRUE.equals(request.getClosed())) {
            if (request.getOpenTime() != null || request.getCloseTime() != null) {
                throw new BadRequestException("Closed days must not include open time or close time");
            }
            return;
        }
        if (request.getOpenTime() == null || request.getCloseTime() == null) {
            throw new BadRequestException("Open time and close time are required for open days");
        }
        if (!request.getCloseTime().isAfter(request.getOpenTime())) {
            throw new BadRequestException("Close time must be after open time");
        }
    }

}
