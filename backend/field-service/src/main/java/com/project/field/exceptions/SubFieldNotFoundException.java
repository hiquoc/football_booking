package com.project.field.exceptions;

import com.project.common.exception.NotFoundException;

import java.util.UUID;

public class SubFieldNotFoundException extends NotFoundException {
    public SubFieldNotFoundException(UUID id) {
        super("SubField not found with id: " + id);
    }
}
