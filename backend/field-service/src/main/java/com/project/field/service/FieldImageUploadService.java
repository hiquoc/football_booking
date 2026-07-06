package com.project.field.service;

import com.project.field.dto.*;
import java.util.List;
import java.util.UUID;

public interface FieldImageUploadService {
    List<ImageUploadSlotDto> issueSlots(UUID fieldId, UUID ownerId, ImageUploadSlotRequest request);
    List<FieldImageDto> confirmBatch(UUID fieldId, UUID ownerId, ImageUploadBatchConfirmRequest request);
    void cleanupStalePlaceholders();
}
