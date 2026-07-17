package com.project.booking.community.dto;

import com.project.booking.community.enums.CommunityPostStatus;
import com.project.booking.community.enums.CommunityPostType;
import com.project.common.enums.SubFieldType;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
public class CommunityPostSearchRequest {
    private CommunityPostType postType;
    private String skillLevel;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate date;
    private SubFieldType fieldType;
    private String district;
    private CommunityPostStatus status;
    private String keyword;
    private String sortBy = "newest";
}
