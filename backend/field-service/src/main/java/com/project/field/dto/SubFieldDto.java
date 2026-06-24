package com.project.field.dto;

import com.project.common.enums.SubFieldType;
import com.project.common.enums.SportType;
import com.project.field.enums.IndoorOutdoor;
import com.project.field.enums.SurfaceType;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubFieldDto {
    private UUID id;
    private UUID fieldId;
    private SportType fieldType;
    private String name;
    private String description;
    private Boolean active;
    private LocalDate bookingDisabledFrom;
    private IndoorOutdoor indoorOutdoor;
    private SurfaceType surfaceType;
    private SubFieldType subFieldType;
    private Integer maxPlayers;
    private Boolean lighting;
    private Boolean parking;
    private Boolean changingRoom;
    private Boolean shower;
    private Boolean wifi;
    private Boolean airConditioning;
    private BookingRuleDto bookingRule;
    private List<TimePriceRuleDto> timePriceRules;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
