package com.project.field.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewDto {
    private UUID id;
    private UUID fieldId;
    private UUID userId;
    private Integer rating;
    private String comment;
    private LocalDateTime createdAt;
}
