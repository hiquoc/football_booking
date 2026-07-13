package com.project.user.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.project.common.cache.CacheNames;
import com.project.common.exception.*;
import com.project.user.dto.*;
import com.project.user.entity.*;
import com.project.user.mapper.UserMapper;
import com.project.user.repository.*;
import com.project.user.service.AvatarUploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.*;
import java.util.*;

@Service @RequiredArgsConstructor
public class AvatarUploadServiceImpl implements AvatarUploadService {
    private final UserRepository users;
    private final AvatarUploadRepository uploads;
    private final IssuedAvatarPublicIdRepository issuedIds;
    private final UserMapper mapper;
    private final Cloudinary cloudinary;

    @Override @Transactional
    public AvatarUploadSlotDto issueSlot(UUID userId, AvatarUploadSlotRequest request) {
        users.findForUpdateById(userId).orElseThrow(() -> new NotFoundException("User not found"));
        return uploads.findByUserIdAndRequestId(userId, request.requestId()).map(this::slot).orElseGet(() -> {
            String publicId;
            do publicId = "avatars/" + UUID.randomUUID(); while (issuedIds.existsById(publicId));
            issuedIds.save(new IssuedAvatarPublicId(publicId, LocalDateTime.now()));
            return slot(uploads.save(AvatarUpload.builder().userId(userId).requestId(request.requestId())
                    .publicId(publicId).uploadTimestamp(Instant.now().getEpochSecond())
                    .createdAt(LocalDateTime.now()).build()));
        });
    }

    @Override @Transactional
    @CacheEvict(cacheNames = CacheNames.USER_BY_ID, key = "'user:' + #userId")
    public UserDto confirm(UUID userId, AvatarUploadConfirmRequest request) {
        AvatarUpload upload = uploads.findByUserIdAndPublicId(userId, request.publicId())
                .orElseThrow(() -> new NotFoundException("Avatar upload placeholder not found"));
        validate(request);
        User user = users.findForUpdateById(userId).orElseThrow(() -> new NotFoundException("User not found"));
        if (upload.getSecureUrl() != null) {
            if (!upload.getSecureUrl().equals(request.secureUrl()) || !Objects.equals(upload.getAssetVersion(), request.version()))
                throw new BadRequestException("This avatar upload was already confirmed with different metadata");
            return mapper.toDto(user);
        }
        String oldPublicId = user.getAvatarPublicId();
        upload.setSecureUrl(request.secureUrl()); upload.setAssetVersion(request.version());
        upload.setImageFormat(request.format().toLowerCase(Locale.ROOT)); upload.setWidth(request.width());
        upload.setHeight(request.height()); upload.setByteSize(request.bytes()); upload.setConfirmedAt(LocalDateTime.now());
        user.setAvatarUrl(request.secureUrl()); user.setAvatarPublicId(request.publicId());
        uploads.save(upload); users.save(user);
        if (oldPublicId != null && !oldPublicId.equals(request.publicId())) destroy(oldPublicId);
        return mapper.toDto(user);
    }

    @Override @Transactional
    public void cleanupStaleUploads() {
        for (AvatarUpload upload : uploads.findBySecureUrlIsNullAndCreatedAtBefore(LocalDateTime.now().minusHours(1))) {
            destroy(upload.getPublicId()); uploads.delete(upload);
        }
    }

    private AvatarUploadSlotDto slot(AvatarUpload upload) {
        Map<String,Object> params = new TreeMap<>();
        params.put("overwrite", false); params.put("public_id", upload.getPublicId()); params.put("timestamp", upload.getUploadTimestamp());
        String signature = cloudinary.apiSignRequest(params, cloudinary.config.apiSecret);
        return new AvatarUploadSlotDto(upload.getPublicId(), upload.getUploadTimestamp(), signature,
                cloudinary.config.apiKey, cloudinary.config.cloudName,
                "https://api.cloudinary.com/v1_1/" + cloudinary.config.cloudName + "/image/upload", false);
    }

    private void validate(AvatarUploadConfirmRequest request) {
        String expected = cloudinary.apiSignRequest(Map.of("public_id", request.publicId(), "version", request.version()), cloudinary.config.apiSecret);
        if (!MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), request.signature().getBytes(StandardCharsets.UTF_8)))
            throw new BadRequestException("Invalid Cloudinary upload result signature");
        try {
            URI uri = URI.create(request.secureUrl());
            if (!"https".equalsIgnoreCase(uri.getScheme()) || !"res.cloudinary.com".equalsIgnoreCase(uri.getHost())
                    || !uri.getPath().startsWith("/" + cloudinary.config.cloudName + "/image/upload/")
                    || !uri.getPath().matches(".*/" + request.publicId() + "\\.[A-Za-z0-9]+$")) throw new IllegalArgumentException();
        } catch (IllegalArgumentException e) { throw new BadRequestException("Invalid Cloudinary secure URL"); }
    }

    private void destroy(String publicId) {
        try { cloudinary.uploader().destroy(publicId, ObjectUtils.asMap("invalidate", true)); }
        catch (Exception e) { throw new IllegalStateException("Failed to delete Cloudinary avatar", e); }
    }
}
