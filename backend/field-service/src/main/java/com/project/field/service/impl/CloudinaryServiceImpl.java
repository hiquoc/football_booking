package com.project.field.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.project.common.exception.BadRequestException;
import com.project.field.exceptions.ImageStorageException;
import com.project.field.service.CloudinaryService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Service
@RequiredArgsConstructor
@Slf4j
public class CloudinaryServiceImpl implements CloudinaryService {

    private static final Set<String> SUPPORTED_IMAGE_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

    private final Cloudinary cloudinary;

    @Override
    public String uploadImage(MultipartFile file) {
        validateImage(file);
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.emptyMap());
            return (String) uploadResult.get("url");
        } catch (IOException e) {
            log.error("Failed to upload image to Cloudinary", e);
            throw new ImageStorageException("Image storage provider failed to upload the image", e);
        }
    }

    @Override
    public List<String> uploadImages(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            throw new BadRequestException("At least one image file is required");
        }

        files.forEach(this::validateImage);
        List<String> uploadedUrls = new ArrayList<>();
        try {
            for (MultipartFile file : files) {
                uploadedUrls.add(uploadImage(file));
            }
            return uploadedUrls;
        } catch (ImageStorageException uploadException) {
            try {
                deleteImages(uploadedUrls);
            } catch (ImageStorageException cleanupException) {
                uploadException.addSuppressed(cleanupException);
                log.error("Failed to clean up partially uploaded images", cleanupException);
            }
            throw uploadException;
        }
    }

    private void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Image files must not be empty");
        }
        if (!SUPPORTED_IMAGE_TYPES.contains(file.getContentType())) {
            throw new BadRequestException("Only JPEG, PNG, and WEBP images are supported");
        }
    }

    @Override
    public void deleteImage(String imageUrl) {
        try {
            String publicId = extractPublicId(imageUrl);
            if (publicId != null) {
                cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
            }
        } catch (IOException e) {
            log.error("Failed to delete image from Cloudinary", e);
        }
    }

    @Override
    public void deleteImages(List<String> imageUrls) {
        if (imageUrls == null || imageUrls.isEmpty()) {
            return;
        }

        List<String> publicIds = imageUrls.stream()
                .map(this::extractPublicId)
                .filter(publicId -> publicId != null && !publicId.isBlank())
                .distinct()
                .toList();
        if (publicIds.isEmpty()) {
            return;
        }

        try {
            cloudinary.api().deleteResources(publicIds, ObjectUtils.asMap("invalidate", true));
        } catch (Exception e) {
            log.error("Failed to batch delete {} images from Cloudinary", publicIds.size(), e);
            throw new ImageStorageException("Image storage provider failed to delete uploaded images", e);
        }
    }

    @Override
    public String extractPublicId(String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            return null;
        }
        try {
            int lastSlashIndex = imageUrl.lastIndexOf('/');
            int lastDotIndex = imageUrl.lastIndexOf('.');
            if (lastSlashIndex != -1 && lastDotIndex != -1 && lastSlashIndex < lastDotIndex) {
                return imageUrl.substring(lastSlashIndex + 1, lastDotIndex);
            }
        } catch (Exception e) {
            log.error("Error extracting public id from image url: {}", imageUrl, e);
        }
        return null;
    }

    @Override
    public String sign(Map<String, Object> parameters) {
        return cloudinary.apiSignRequest(parameters, cloudinary.config.apiSecret);
    }

    @Override
    public boolean verifyUploadResult(String publicId, long version, String signature) {
        String expected = sign(Map.of("public_id", publicId, "version", version));
        return signature != null && MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8), signature.getBytes(StandardCharsets.UTF_8));
    }

    @Override public String apiKey() { return cloudinary.config.apiKey; }
    @Override public String cloudName() { return cloudinary.config.cloudName; }
    @Override public String uploadUrl() {
        return "https://api.cloudinary.com/v1_1/" + cloudName() + "/image/upload";
    }

    @Override
    public void deleteByPublicId(String publicId) {
        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.asMap("invalidate", true));
        } catch (IOException e) {
            throw new ImageStorageException("Image storage provider failed to delete the image", e);
        }
    }
}
