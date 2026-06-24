package com.project.field.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.project.common.exception.BadRequestException;
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
            throw new BadRequestException("Failed to upload image");
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
        } catch (RuntimeException ex) {
            uploadedUrls.forEach(this::deleteImage);
            throw ex;
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
}
