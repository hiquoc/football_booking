package com.project.field.service;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

public interface CloudinaryService {
    String uploadImage(MultipartFile file);

    List<String> uploadImages(List<MultipartFile> files);

    void deleteImage(String imageUrl);

    void deleteImages(List<String> imageUrls);

    String extractPublicId(String imageUrl);

    String sign(Map<String, Object> parameters);
    boolean verifyUploadResult(String publicId, long version, String signature);
    String apiKey();
    String cloudName();
    String uploadUrl();
    void deleteByPublicId(String publicId);
}
