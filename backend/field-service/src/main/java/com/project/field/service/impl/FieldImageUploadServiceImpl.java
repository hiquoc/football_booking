package com.project.field.service.impl;

import com.project.common.cache.CacheNames;
import com.project.common.exception.*;
import com.project.field.dto.*;
import com.project.field.entity.*;
import com.project.field.exceptions.FieldNotFoundException;
import com.project.field.mapper.FieldMapper;
import com.project.field.repository.*;
import com.project.field.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.net.URI;
import java.time.*;
import java.util.*;

@Service
@RequiredArgsConstructor
public class FieldImageUploadServiceImpl implements FieldImageUploadService {
    private static final long DEFAULT_MAX_IMAGE_BYTES = 10L * 1024L * 1024L;

    private final FieldRepository fieldRepository;
    private final FieldImageRepository imageRepository;
    private final CloudinaryService cloudinary;
    private final FieldMapper mapper;
    private final PlatformTransactionManager transactionManager;

    @Value("${cloudinary.max-image-bytes:" + DEFAULT_MAX_IMAGE_BYTES + "}")
    private long maxImageBytes;

    @Override
    public List<ImageUploadSlotDto> issueSlots(UUID fieldId, UUID ownerId, ImageUploadSlotRequest request) {
        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                return Objects.requireNonNull(new TransactionTemplate(transactionManager).execute(
                        status -> issueSlotsAttempt(fieldId, ownerId, request)));
            } catch (DataIntegrityViolationException ex) {
                List<FieldImage> idempotentResult = imageRepository
                        .findByFieldIdAndUploadOwnerIdAndUploadRequestIdOrderByUploadSlotIndex(
                                fieldId, ownerId, request.requestId());
                if (!idempotentResult.isEmpty()) {
                    validateExistingRequest(idempotentResult, request.count());
                    return idempotentResult.stream().map(this::toSlot).toList();
                }
                if (!isUniquePublicIdViolation(ex)) throw ex;
            }
        }
        throw new IllegalStateException("Unable to generate unique public IDs");
    }

    private List<ImageUploadSlotDto> issueSlotsAttempt(
            UUID fieldId, UUID ownerId, ImageUploadSlotRequest request) {
        Field field = fieldRepository.findByIdForUpdate(fieldId)
                .orElseThrow(() -> new FieldNotFoundException(fieldId));
        requireOwner(field, ownerId);

        List<FieldImage> existing = imageRepository
                .findByFieldIdAndUploadOwnerIdAndUploadRequestIdOrderByUploadSlotIndex(fieldId, ownerId, request.requestId());
        if (!existing.isEmpty()) {
            validateExistingRequest(existing, request.count());
            return existing.stream().map(this::toSlot).toList();
        }

        int nextOrder = field.getImages().stream().map(FieldImage::getDisplayOrder)
                .filter(Objects::nonNull).max(Integer::compareTo).orElse(-1) + 1;
        long timestamp = Instant.now().getEpochSecond();
        List<FieldImage> placeholders = new ArrayList<>(request.count());
        for (int i = 0; i < request.count(); i++) {
            placeholders.add(FieldImage.builder()
                    .field(field).publicId(UUID.randomUUID().toString()).uploadOwnerId(ownerId)
                    .uploadRequestId(request.requestId()).uploadSlotIndex(i).uploadTimestamp(timestamp)
                    .createdAt(LocalDateTime.now()).isPrimary(false).displayOrder(nextOrder + i).build());
        }
        return imageRepository.saveAllAndFlush(placeholders).stream().map(this::toSlot).toList();
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = {CacheNames.FIELD_DETAIL, CacheNames.FIELD_SEARCH}, allEntries = true)
    public List<FieldImageDto> confirmBatch(UUID fieldId, UUID ownerId, ImageUploadBatchConfirmRequest request) {
        Map<String, ImageUploadConfirmRequest> requestsByPublicId = new LinkedHashMap<>();
        for (ImageUploadConfirmRequest upload : request.uploads()) {
            if (requestsByPublicId.putIfAbsent(upload.publicId(), upload) != null) {
                throw new BadRequestException("Upload public IDs must not contain duplicates");
            }
        }
        Map<String, FieldImage> imagesByPublicId = new HashMap<>();
        requestsByPublicId.keySet().stream().sorted().forEach(publicId -> {
            FieldImage image = imageRepository.findByFieldIdAndPublicId(fieldId, publicId)
                    .orElseThrow(() -> new NotFoundException("Upload placeholder not found: " + publicId));
            if (!ownerId.equals(image.getUploadOwnerId()) || !ownerId.equals(image.getField().getOwnerId()))
                throw new ForbiddenException("You don't have permission to confirm this upload");
            imagesByPublicId.put(publicId, image);
        });
        for (ImageUploadConfirmRequest upload : request.uploads()) {
            FieldImage image = imagesByPublicId.get(upload.publicId());
            validateCloudinaryResult(upload);
            if (image.getImageUrl() != null) {
                if (!sameResult(image, upload))
                    throw new BadRequestException("An upload was already confirmed with different metadata");
                continue;
            }
            applyResult(image, upload);
        }
        imageRepository.saveAll(imagesByPublicId.values());
        return request.uploads().stream().map(upload -> mapper.toImageDto(imagesByPublicId.get(upload.publicId()))).toList();
    }

    @Override
    @Transactional
    public void cleanupStalePlaceholders() {
        List<FieldImage> stale = imageRepository.findByImageUrlIsNullAndCreatedAtBefore(LocalDateTime.now().minusHours(1));
        for (FieldImage image : stale) {
            cloudinary.deleteByPublicId(image.getPublicId());
            imageRepository.delete(image);
        }
    }

    private void applyResult(FieldImage image, ImageUploadConfirmRequest request) {
        image.setImageUrl(request.secureUrl()); image.setAssetVersion(request.version());
        image.setImageFormat(request.format().toLowerCase(Locale.ROOT)); image.setWidth(request.width());
        image.setHeight(request.height()); image.setByteSize(request.bytes()); image.setConfirmedAt(LocalDateTime.now());
    }

    private void validateExistingRequest(List<FieldImage> existing, int count) {
        if (existing.size() != count)
            throw new BadRequestException("The requestId was already used with a different image count");
    }

    private boolean isUniquePublicIdViolation(DataIntegrityViolationException ex) {
        Throwable cause = ex;
        while (cause != null) {
            String message = cause.getMessage();
            if (message != null && message.toLowerCase(Locale.ROOT).contains("uk_field_images_public_id")) return true;
            cause = cause.getCause();
        }
        return false;
    }

    private ImageUploadSlotDto toSlot(FieldImage image) {
        Map<String, Object> params = new TreeMap<>();
        params.put("overwrite", false);
        params.put("public_id", image.getPublicId());
        params.put("timestamp", image.getUploadTimestamp());
        return new ImageUploadSlotDto(image.getId(), image.getPublicId(), image.getUploadTimestamp(),
                cloudinary.sign(params), cloudinary.apiKey(), cloudinary.cloudName(), cloudinary.uploadUrl(), false);
    }

    private void validateCloudinaryResult(ImageUploadConfirmRequest request) {
        if (request.bytes() > maxImageBytes) {
            throw new BadRequestException("Image exceeds the maximum allowed size of " + formatBytes(maxImageBytes));
        }
        if (!cloudinary.verifyUploadResult(request.publicId(), request.version(), request.signature())) {
            throw new BadRequestException("Invalid Cloudinary upload result signature");
        }
        try {
            URI uri = URI.create(request.secureUrl());
            String expectedHost = "res.cloudinary.com";
            String expectedPath = "/" + cloudinary.cloudName() + "/image/upload/";
            if (!"https".equalsIgnoreCase(uri.getScheme()) || !expectedHost.equalsIgnoreCase(uri.getHost())
                    || !uri.getPath().startsWith(expectedPath)
                    || !uri.getPath().matches(".*/" + request.publicId() + "\\.[A-Za-z0-9]+$")) {
                throw new IllegalArgumentException();
            }
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Invalid Cloudinary secure URL");
        }
    }

    private boolean sameResult(FieldImage image, ImageUploadConfirmRequest request) {
        return image.getImageUrl().equals(request.secureUrl()) && Objects.equals(image.getAssetVersion(), request.version())
                && Objects.equals(image.getImageFormat(), request.format().toLowerCase(Locale.ROOT))
                && Objects.equals(image.getWidth(), request.width()) && Objects.equals(image.getHeight(), request.height())
                && Objects.equals(image.getByteSize(), request.bytes());
    }

    private void requireOwner(Field field, UUID ownerId) {
        if (!ownerId.equals(field.getOwnerId())) throw new ForbiddenException("You don't have permission to do this");
    }

    private String formatBytes(long bytes) {
        if (bytes % (1024L * 1024L) == 0) {
            return (bytes / (1024L * 1024L)) + " MB";
        }
        return bytes + " bytes";
    }
}
