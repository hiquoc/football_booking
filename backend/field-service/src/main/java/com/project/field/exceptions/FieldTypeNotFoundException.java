package com.project.field.exceptions;

import com.project.common.exception.NotFoundException;

public class FieldTypeNotFoundException extends NotFoundException {
    public FieldTypeNotFoundException(Long id) {
        super("FieldType not found with id: " + id);
    }
}
