package com.project.field.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class FieldEmployeeDto {
    private UUID assignmentId;
    private UUID fieldId;
    private UUID employeeId;
    private String phoneNumber;
    private String fullName;
    private String email;
    private String avatarUrl;
    private LocalDateTime assignedAt;
}
