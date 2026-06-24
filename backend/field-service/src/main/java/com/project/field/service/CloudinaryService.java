package com.project.field.service;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface CloudinaryService {
    String uploadImage(MultipartFile file);

    List<String> uploadImages(List<MultipartFile> files);

    void deleteImage(String imageUrl);

    String extractPublicId(String imageUrl);
}
