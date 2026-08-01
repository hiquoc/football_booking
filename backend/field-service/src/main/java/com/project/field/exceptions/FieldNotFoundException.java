package com.project.field.exceptions;

import com.project.common.exception.NotFoundException;

import java.util.UUID;

public class FieldNotFoundException extends NotFoundException {
    public FieldNotFoundException(UUID id) {
        super("Field not found with id: " + id, "FIELD_NOT_FOUND");
    }
}
