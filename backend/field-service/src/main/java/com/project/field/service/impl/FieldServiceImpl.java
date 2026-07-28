package com.project.field.service.impl;

import com.project.common.dto.ApiResponse;
import com.project.common.dto.PageResponse;
import com.project.common.cache.CacheKeys;
import com.project.common.cache.CacheNames;
import com.project.common.enums.UserType;
import com.project.common.exception.BadRequestException;
import com.project.common.exception.ForbiddenException;
import com.project.common.exception.NotFoundException;
import com.project.common.security.UserPrincipal;
import com.project.field.client.UserServiceClient;
import com.project.field.dto.FieldDto;
import com.project.field.dto.FieldCardDto;
import com.project.field.dto.FieldImageDto;
import com.project.field.dto.FieldImageOrderRequest;
import com.project.field.dto.FieldRequest;
import com.project.field.dto.OperatingHoursRequest;
import com.project.field.dto.UserDto;
import com.project.field.entity.Field;
import com.project.field.entity.FieldImage;
import com.project.field.entity.FieldOperatingHours;
import com.project.field.entity.SubField;
import com.project.field.exceptions.FieldNotFoundException;
import com.project.field.enums.FieldStatus;
import com.project.field.kafka.FieldEventPublisher;
import com.project.field.mapper.FieldMapper;
import com.project.field.repository.FieldImageRepository;
import com.project.field.repository.FieldOperatingHoursRepository;
import com.project.field.repository.FieldRepository;
import com.project.field.repository.FieldCardQueryRepository;
import com.project.field.repository.FieldFavoriteRepository;
import com.project.field.repository.SubFieldRepository;
import com.project.field.service.CloudinaryService;
import com.project.field.service.FieldService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FieldServiceImpl implements FieldService {

    private final FieldRepository fieldRepository;
    private final FieldCardQueryRepository fieldCardQueryRepository;
    private final FieldImageRepository fieldImageRepository;
    private final FieldFavoriteRepository fieldFavoriteRepository;
    private final FieldOperatingHoursRepository fieldOperatingHoursRepository;
    private final SubFieldRepository subFieldRepository;
    private final FieldMapper fieldMapper;
    private final UserServiceClient userServiceClient;
    private final CloudinaryService cloudinaryService;
    private final FieldEventPublisher fieldEventPublisher;
    private final OperatingHoursPriceRuleSynchronizer operatingHoursPriceRuleSynchronizer;

    @Override
    @Transactional
    @CacheEvict(cacheNames = {CacheNames.FIELD_DETAIL, CacheNames.FIELD_SEARCH}, allEntries = true)
    public FieldDto create(UUID ownerId, FieldRequest request) {
        validateCreateRequest(request);
        validate(ownerId);

        Field field = fieldMapper.toEntity(ownerId, request);

        Field saved = fieldRepository.save(field);
        fieldEventPublisher.publishFieldOperatingHoursUpdated(List.of(), createOperatingHours(saved, request), List.of());
        return fieldMapper.toDto(saved);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = {CacheNames.FIELD_DETAIL, CacheNames.FIELD_SEARCH}, allEntries = true)
    public FieldDto update(UUID id, UUID ownerId, FieldRequest request) {
        Field field = fieldRepository.findById(id)
                .orElseThrow(() -> new FieldNotFoundException(id));
        if (!field.getOwnerId().equals(ownerId)) {
            throw new ForbiddenException("You don't have permission to do this");
        }
        validateUpdateRequest(request, field);

        // Use mapper to update only non-null fields
        fieldMapper.updateEntity(field, request);

        Field saved = fieldRepository.save(field);
        if (request.getOperatingHours() != null) {
            List<FieldOperatingHours> previousHours = fieldOperatingHoursRepository.findByFieldId(id).stream()
                    .map(this::copyOperatingHours)
                    .toList();
            List<FieldOperatingHours> operatingHours = upsertOperatingHours(saved, request);
            List<SubField> affectedSubFields = subFieldRepository.findByFieldId(id);
            List<SubField> updatedSubFields = operatingHoursPriceRuleSynchronizer
                    .synchronizeFieldHours(affectedSubFields, operatingHours);
            fieldEventPublisher.publishFieldOperatingHoursUpdated(
                    previousHours,
                    operatingHours,
                    affectedSubFields.stream().map(SubField::getId).toList());
            updatedSubFields.forEach(fieldEventPublisher::publishTimePriceRulesChanged);
        }
        return fieldMapper.toDto(saved);
    }

    @Override
    @Cacheable(cacheNames = CacheNames.FIELD_DETAIL, key = "'field:' + #id + ':viewer:internal'", sync = true)
    public FieldDto getById(UUID id) {
        return fieldRepository.findById(id)
                .map(fieldMapper::toDto)
                .orElseThrow(() -> new FieldNotFoundException(id));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CacheNames.FIELD_DETAIL, key = CacheKeys.FIELD_DETAIL, sync = true)
    public FieldDto getWithDetailsById(UUID id, UserPrincipal userPrincipal) {
        Field field = fieldRepository.findWithDetailsById(id)
                .orElseThrow(() -> new FieldNotFoundException(id));
        if (!canViewField(field, userPrincipal)) {
            throw new ForbiddenException("You don't have permission to do this");
        }
        field.setImages(fieldImageRepository.findByFieldIdAndImageUrlIsNotNull(id));
        field.setSubFields(subFieldRepository.findByFieldId(id));
        return fieldMapper.toDto(field, isFavorite(userPrincipal, id));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<FieldDto> getByOwnerId(UUID ownerId, Pageable pageable) {
        return PageResponse.from(fieldRepository.findByOwnerId(ownerId, pageable).map(fieldMapper::toDto));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<FieldDto> getAll(FieldStatus status, Pageable pageable, UserPrincipal currentUser) {
        if (status != null) {
            return toFavoriteAwarePage(fieldRepository.findByStatus(status, pageable), currentUser);
        }
        return toFavoriteAwarePage(fieldRepository.findByStatusAndActiveTrue(FieldStatus.APPROVED, pageable), currentUser);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = {CacheNames.FIELD_DETAIL, CacheNames.FIELD_SEARCH}, allEntries = true)
    public FieldDto updateStatus(UUID id, FieldStatus status) {
        Field field = fieldRepository.findById(id)
                .orElseThrow(() -> new FieldNotFoundException(id));
        field.setStatus(status);
        return fieldMapper.toDto(fieldRepository.save(field));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CacheNames.FIELD_SEARCH, key = CacheKeys.FIELD_SEARCH, sync = true)
    public PageResponse<FieldCardDto> searchCards(String keyword, String fieldType, String subFieldType, String district, String provinceCode,
                                                  BigDecimal latitude, BigDecimal longitude, Double radiusKm, String sortBy, String direction,
                                                  int page, int size, UserPrincipal currentUser) {
        return PageResponse.from(fieldCardQueryRepository.search(keyword, fieldType, subFieldType, district, provinceCode,
                latitude, longitude, radiusKm, sortBy, direction, page, size,
                currentUser != null && isClientLike(currentUser.role()) ? currentUser.id() : null));
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = {CacheNames.FIELD_DETAIL, CacheNames.FIELD_SEARCH}, allEntries = true)
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
    @CacheEvict(cacheNames = {CacheNames.FIELD_DETAIL, CacheNames.FIELD_SEARCH}, allEntries = true)
    public List<FieldImageDto> uploadImages(UUID fieldId, List<MultipartFile> files) {
        List<String> imageUrls = cloudinaryService.uploadImages(files);
        try {
            return addImages(fieldId, imageUrls);
        } catch (RuntimeException ex) {
            try {
                cloudinaryService.deleteImages(imageUrls);
            } catch (RuntimeException cleanupException) {
                ex.addSuppressed(cleanupException);
            }
            throw ex;
        }
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = {CacheNames.FIELD_DETAIL, CacheNames.FIELD_SEARCH}, allEntries = true)
    public void deleteImage(Long imageId) {
        fieldImageRepository.deleteById(imageId);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = {CacheNames.FIELD_DETAIL, CacheNames.FIELD_SEARCH}, allEntries = true)
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

    private boolean canViewField(Field field, UserPrincipal userPrincipal) {
        if (field.getStatus() == FieldStatus.APPROVED) {
            return true;
        }
        if (userPrincipal == null) {
            return false;
        }
        if ("ADMIN".equals(userPrincipal.role())) {
            return true;
        }
        return field.getOwnerId().equals(userPrincipal.id());
    }

    private Boolean isFavorite(UserPrincipal userPrincipal, UUID fieldId) {
        if (userPrincipal == null || userPrincipal.id() == null || !isClientLike(userPrincipal.role())) {
            return false;
        }
        return fieldFavoriteRepository.existsByUserIdAndFieldId(userPrincipal.id(), fieldId);
    }

    private PageResponse<FieldDto> toFavoriteAwarePage(org.springframework.data.domain.Page<Field> fields,
                                                       UserPrincipal currentUser) {
        if (currentUser == null || currentUser.id() == null || !isClientLike(currentUser.role())) {
            return PageResponse.from(fields.map(field -> fieldMapper.toDto(field, false)));
        }
        List<UUID> fieldIds = fields.getContent().stream().map(Field::getId).toList();
        Set<UUID> favoriteIds = fieldIds.isEmpty()
                ? Set.of()
                : new HashSet<>(fieldFavoriteRepository.findFavoriteFieldIds(currentUser.id(), fieldIds));
        return PageResponse.from(fields.map(field -> fieldMapper.toDto(field, favoriteIds.contains(field.getId()))));
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
                    operatingHours.setOpen24Hours(!Boolean.TRUE.equals(request.getClosed()) && Boolean.TRUE.equals(request.getOpen24Hours()));
                    operatingHours.setOpenTime(Boolean.TRUE.equals(request.getClosed()) || Boolean.TRUE.equals(request.getOpen24Hours()) ? null : request.getOpenTime());
                    operatingHours.setCloseTime(Boolean.TRUE.equals(request.getClosed()) || Boolean.TRUE.equals(request.getOpen24Hours()) ? null : request.getCloseTime());
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
        operatingHours.setOpen24Hours(!Boolean.TRUE.equals(request.getClosed()) && Boolean.TRUE.equals(request.getOpen24Hours()));
        operatingHours.setOpenTime(Boolean.TRUE.equals(request.getClosed()) || Boolean.TRUE.equals(request.getOpen24Hours()) ? null : request.getOpenTime());
        operatingHours.setCloseTime(Boolean.TRUE.equals(request.getClosed()) || Boolean.TRUE.equals(request.getOpen24Hours()) ? null : request.getCloseTime());
        operatingHours.setClosed(Boolean.TRUE.equals(request.getClosed()));
        return operatingHours;
    }

    // ─── Private helpers ────────────────────────────────────────────────────────

    private FieldOperatingHours copyOperatingHours(FieldOperatingHours hours) {
        return FieldOperatingHours.builder()
                .id(hours.getId())
                .fieldId(hours.getFieldId())
                .dayOfWeek(hours.getDayOfWeek())
                .openTime(hours.getOpenTime())
                .closeTime(hours.getCloseTime())
                .closed(hours.getClosed())
                .open24Hours(hours.getOpen24Hours())
                .build();
    }

    private void validate(UUID ownerId) {
        ApiResponse<UserDto> response = userServiceClient.getUserProfile(ownerId);
        if (response == null || !response.isSuccess() || response.getData() == null) {
            throw new NotFoundException("User not found with id " + ownerId);
        }
        if (response.getData().getUserType() != UserType.OWNER) {
            throw new ForbiddenException("You don't have permission to do this operation");
        }
    }

    private boolean isClientLike(String role) {
        return "CLIENT".equals(role) || "EMPLOYEE".equals(role);
    }

    private void validateCreateRequest(FieldRequest request) {
        if (request.getName() == null || request.getName().isBlank()) {
            throw new BadRequestException("Field name is required");
        }
        if (request.getAddress() == null || request.getAddress().isBlank()) {
            throw new BadRequestException("Field address is required");
        }
        requireLocationValue(request.getWard(), "Ward");
        requireLocationValue(request.getWardCode(), "Ward code");
        requireLocationValue(request.getProvince(), "Province");
        requireLocationValue(request.getProvinceCode(), "Province code");
        requireLocationValue(request.getLegacyWard(), "Legacy ward");
        requireLocationValue(request.getLegacyWardCode(), "Legacy ward code");
        requireLocationValue(request.getLegacyDistrict(), "Legacy district");
        requireLocationValue(request.getLegacyProvince(), "Legacy province");
        if (request.getLatitude() == null || request.getLongitude() == null) {
            throw new BadRequestException("Field location must be selected on the map");
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
        rejectBlankLocationValue(request.getWard(), "Ward");
        rejectBlankLocationValue(request.getWardCode(), "Ward code");
        rejectBlankLocationValue(request.getProvince(), "Province");
        rejectBlankLocationValue(request.getProvinceCode(), "Province code");
        rejectBlankLocationValue(request.getLegacyWard(), "Legacy ward");
        rejectBlankLocationValue(request.getLegacyWardCode(), "Legacy ward code");
        rejectBlankLocationValue(request.getLegacyDistrict(), "Legacy district");
        rejectBlankLocationValue(request.getLegacyProvince(), "Legacy province");
        if (request.getPhoneNumber() != null && request.getPhoneNumber().isBlank()) {
            throw new BadRequestException("Phone number must not be blank");
        }
        validateOperatingHours(request, false);
    }

    private void requireLocationValue(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException(label + " is required");
        }
    }

    private void rejectBlankLocationValue(String value, String label) {
        if (value != null && value.isBlank()) {
            throw new BadRequestException(label + " must not be blank");
        }
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
            if (request.getOpenTime() != null || request.getCloseTime() != null || Boolean.TRUE.equals(request.getOpen24Hours())) {
//                throw new BadRequestException("Closed days must not include open time or close time");
                request.setOpenTime(null);
                request.setCloseTime(null);
                request.setOpen24Hours(false);
            }
            return;
        }
        if (Boolean.TRUE.equals(request.getOpen24Hours())) {
            request.setOpenTime(null);
            request.setCloseTime(null);
            return;
        }
        if (request.getOpenTime() == null || request.getCloseTime() == null) {
            throw new BadRequestException("Open time and close time are required for open days");
        }
    }

}
