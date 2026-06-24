package com.project.field.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FieldRequestValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void updateAllowsOmittedFields() {
        FieldRequest request = FieldRequest.builder()
                .description("Updated description")
                .build();

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void createStillRequiresMandatoryFields() {
        FieldRequest request = new FieldRequest();

        assertThat(validator.validate(request, FieldRequest.Create.class))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("name", "address", "phoneNumber");
    }
}
