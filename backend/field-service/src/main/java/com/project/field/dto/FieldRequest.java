package com.project.field.dto;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
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

    @NotBlank(message = "Ward is required", groups = Create.class)
    @Pattern(regexp = ".*\\S.*", message = "Ward must not be blank")
    private String ward;

    @NotBlank(message = "Ward code is required", groups = Create.class)
    @Size(max = 20, message = "Ward code must not exceed 20 characters")
    private String wardCode;

    @NotBlank(message = "Province is required", groups = Create.class)
    @Pattern(regexp = ".*\\S.*", message = "Province must not be blank")
    private String province;

    @NotBlank(message = "Province code is required", groups = Create.class)
    @Size(max = 20, message = "Province code must not exceed 20 characters")
    private String provinceCode;

    @NotBlank(message = "Legacy ward is required", groups = Create.class)
    @Pattern(regexp = ".*\\S.*", message = "Legacy ward must not be blank")
    private String legacyWard;

    @NotBlank(message = "Legacy ward code is required", groups = Create.class)
    @Size(max = 20, message = "Legacy ward code must not exceed 20 characters")
    private String legacyWardCode;

    @NotBlank(message = "Legacy district is required", groups = Create.class)
    @Pattern(regexp = ".*\\S.*", message = "Legacy district must not be blank")
    private String legacyDistrict;

    @NotBlank(message = "Legacy province is required", groups = Create.class)
    @Pattern(regexp = ".*\\S.*", message = "Legacy province must not be blank")
    private String legacyProvince;

    @NotNull(message = "Latitude is required", groups = Create.class)
    @DecimalMin(value = "-90", message = "Latitude must be at least -90")
    @DecimalMax(value = "90", message = "Latitude must not exceed 90")
    private BigDecimal latitude;

    @NotNull(message = "Longitude is required", groups = Create.class)
    @DecimalMin(value = "-180", message = "Longitude must be at least -180")
    @DecimalMax(value = "180", message = "Longitude must not exceed 180")
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
