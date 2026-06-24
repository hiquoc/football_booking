package com.project.field.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
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
public class FieldRequest {
    public interface Create extends Default {
    }

    @NotBlank(message = "Field name is required", groups = Create.class)
    @Pattern(regexp = ".*\\S.*", message = "Field name must not be blank")
    @Size(max = 100, message = "Field name must not exceed 100 characters")
    private String name;

    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    private String description;

    @NotBlank(message = "Field address is required", groups = Create.class)
    @Pattern(regexp = ".*\\S.*", message = "Field address must not be blank")
    private String address;

    private BigDecimal latitude;

    private BigDecimal longitude;

    @NotBlank(message = "Phone number is required", groups = Create.class)
    @Pattern(regexp = ".*\\S.*", message = "Phone number must not be blank")
    @Size(max = 20, message = "Phone number must not exceed 20 characters")
    private String phoneNumber;

    @Email(message = "Email must be valid")
    @Size(max = 100, message = "Email must not exceed 100 characters")
    private String email;

    @Valid
    @Size(min = 7, max = 7, message = "Operating hours must include exactly 7 days")
    private List<OperatingHoursRequest> operatingHours;

    private Boolean active;

}
