package com.project.user.dto;
public record AvatarUploadSlotDto(String publicId, long timestamp, String signature, String apiKey,
        String cloudName, String uploadUrl, boolean overwrite) {}
