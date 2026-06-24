package com.project.field.dto;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewRequest {
    private UUID fieldId;
    private Integer rating;
    private String comment;
}
