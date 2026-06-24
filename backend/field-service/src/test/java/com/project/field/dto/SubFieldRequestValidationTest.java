package com.project.field.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SubFieldRequestValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void updateAllowsOmittedFields() {
        SubFieldRequest request = SubFieldRequest.builder()
                .build();

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void createRequiresNameAndType() {
        SubFieldRequest request = new SubFieldRequest();

        assertThat(validator.validate(request, SubFieldRequest.Create.class))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("name", "subFieldType");
    }
}
