package com.project.booking.community.dto;

import com.project.booking.community.enums.CommunityPostStatus;
import com.project.booking.community.enums.CommunityPostType;
import com.project.common.enums.SubFieldType;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.UUID;

@Data
public class CommunityPostSearchRequest {
    private UUID ownerId;
    private UUID applicantId;
    private CommunityPostType postType;
    private String skillLevel;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate date;
    private SubFieldType fieldType;
    private String city;
    private String district;
    private String fieldName;
    private CommunityPostStatus status;
    private String keyword;
    private String sortBy = "upcoming";
}
