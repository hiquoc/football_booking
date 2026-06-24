package com.project.field.dto;

import java.time.LocalDate;
import java.util.List;

import com.project.common.enums.SubFieldType;
import com.project.field.enums.IndoorOutdoor;
import com.project.field.enums.SurfaceType;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.validation.groups.Default;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubFieldRequest {
    public interface Create extends Default {
    }

    @NotBlank(message = "Sub-field name is required", groups = Create.class)
    @Pattern(regexp = ".*\\S.*", message = "Sub-field name must not be blank")
    @Size(max = 100, message = "Sub-field name must not exceed 100 characters")
    private String name;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;

    private Boolean active;

    private LocalDate bookingDisabledFrom;

    @NotNull(message = "Sub-field type is required", groups = Create.class)
    private SubFieldType subFieldType;

    private IndoorOutdoor indoorOutdoor;

    private SurfaceType surfaceType;

    private Boolean changingRoom;

    private Boolean shower;

    private Boolean wifi;

    private Boolean airConditioning;

    @Valid
    private BookingRuleDto bookingRule;

    @Valid
    private List<TimePriceRuleDto> timePriceRules;
}
