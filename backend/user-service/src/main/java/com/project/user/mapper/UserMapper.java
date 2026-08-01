package com.project.user.mapper;

import com.project.user.dto.UserDto;
import com.project.user.dto.ProfileInfoDto;
import com.project.user.dto.ProfileReputationDto;
import com.project.user.dto.ProfileStatisticsDto;
import com.project.user.dto.PublicProfileDto;
import com.project.user.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {
    @Mapping(target = "isBookingBanned", ignore = true)
    @Mapping(target = "banExpiresAt", ignore = true)
    @Mapping(target = "isPermanentBan", ignore = true)
    UserDto toDto(User user);

    default UserDto toDtoWithBan(User user) {
        UserDto dto = toDto(user);
        boolean banned = user != null && "PLATFORM_BANNED".equals(user.getStatus());
        dto.setIsBookingBanned(banned);
        dto.setIsPermanentBan(banned);
        dto.setBanExpiresAt(null);
        return dto;
    }

    @Mapping(target = "avatarPublicId", ignore = true)
    @Mapping(target = "teamPhotoPublicId", ignore = true)
    User toEntity(UserDto dto);

    default PublicProfileDto toPublicProfileDto(User user) {
        if (user == null) {
            return null;
        }
        return PublicProfileDto.builder()
                .personal(ProfileInfoDto.builder()
                        .id(user.getId())
                        .fullName(user.getFullName())
                        .avatarUrl(user.getAvatarUrl())
                        .phoneNumber(maskPhoneNumber(user.getPhoneNumber()))
                        .email(maskEmail(user.getEmail()))
                        .bio(user.getBio())
                        .teamPhotoUrl(user.getTeamPhotoUrl())
                        .skillLevel(user.getSkillLevel())
                        .build())
                .statistics(ProfileStatisticsDto.builder()
                        .totalMatches(valueOrZero(user.getTotalMatches()))
                        .wins(valueOrZero(user.getWins()))
                        .draws(valueOrZero(user.getDraws()))
                        .losses(valueOrZero(user.getLosses()))
                        .winRate(calculateWinRate(user))
                        .completedBookingCount(valueOrZero(user.getCompletedBookingCount()))
                        .build())
                .reputation(ProfileReputationDto.builder()
                        .noCancelRate(user.getNoCancelRate())
                        .onTimeRate(user.getOnTimeRate())
                        .fairPlayRate(user.getFairPlayRate())
                        .build())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    default int valueOrZero(Integer value) {
        return value == null ? 0 : value;
    }

    default java.math.BigDecimal calculateWinRate(User user) {
        int totalMatches = user.getTotalMatches() == null ? 0 : user.getTotalMatches();
        int wins = user.getWins() == null ? 0 : user.getWins();
        if (totalMatches == 0) {
            return java.math.BigDecimal.ZERO.setScale(1);
        }
        return java.math.BigDecimal.valueOf(wins)
                .multiply(java.math.BigDecimal.valueOf(100))
                .divide(java.math.BigDecimal.valueOf(totalMatches), 1, java.math.RoundingMode.HALF_UP);
    }

    private String maskPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isBlank()) {
            return null;
        }
        String digits = phoneNumber.replaceAll("\\D", "");
        if (digits.length() >= 4) {
            return "******" + digits.substring(digits.length() - 4);
        }
        if (digits.length() >= 3) {
            return "***" + digits.substring(digits.length() - 3);
        }
        return "***";
    }

    private String maskEmail(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }
        String trimmed = email.trim();
        int atIndex = trimmed.indexOf('@');
        if (atIndex <= 0) {
            return "***";
        }
        String local = trimmed.substring(0, atIndex);
        String domain = trimmed.substring(atIndex);
        String visible = local.substring(0, Math.min(2, local.length()));
        return visible + "***" + domain;
    }
}
