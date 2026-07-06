package com.project.field.controller;

import com.project.common.dto.ApiResponse;
import com.project.field.dto.FieldImageDto;
import com.project.field.dto.FieldImageOrderRequest;
import com.project.field.dto.ImageUploadSlotRequest;
import com.project.field.dto.ImageUploadSlotDto;
import com.project.field.dto.ImageUploadBatchConfirmRequest;
import com.project.field.service.FieldImageUploadService;
import com.project.common.security.CurrentUser;
import com.project.common.security.UserPrincipal;
import com.project.field.service.FieldService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/fields/{fieldId}/images")
@RequiredArgsConstructor
@Tag(name = "Field Images", description = "Upload and manage images for a field using Cloudinary.")
public class FieldImageController {

    private final FieldService fieldService;
    private final FieldImageUploadService imageUploadService;

    @Operation(summary = "Request direct-upload slots",
            description = "Creates idempotent placeholders and returns signed Cloudinary parameters. Reuse requestId when retrying a batch.")
    @PreAuthorize("hasRole('OWNER')")
    @PostMapping("/upload-slots")
    public ApiResponse<List<ImageUploadSlotDto>> issueUploadSlots(
            @PathVariable UUID fieldId, @CurrentUser UserPrincipal currentUser,
            @Valid @RequestBody ImageUploadSlotRequest request) {
        return ApiResponse.success("Upload slots issued successfully",
                imageUploadService.issueSlots(fieldId, currentUser.id(), request));
    }

    @Operation(summary = "Confirm direct Cloudinary uploads as a batch",
            description = "Atomically validates signed Cloudinary results and idempotently completes all owned placeholders.")
    @PreAuthorize("hasRole('OWNER')")
    @PostMapping("/confirm")
    public ApiResponse<List<FieldImageDto>> confirmUpload(
            @PathVariable UUID fieldId, @CurrentUser UserPrincipal currentUser,
            @Valid @RequestBody ImageUploadBatchConfirmRequest request) {
        return ApiResponse.success("Image uploads confirmed successfully",
                imageUploadService.confirmBatch(fieldId, currentUser.id(), request));
    }

    @Operation(
            summary = "Order field images and select the primary image",
            description = "Atomically replaces the image order and selects one cover image. " +
                          "Provide every current image ID exactly once; array position becomes displayOrder starting at 0.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = FieldImageOrderRequest.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "imageIds": [12, 10, 11],
                                      "primaryImageId": 10
                                    }
                                    """))))
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Image order and primary image updated",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "success": true,
                                      "message": "Image order updated successfully",
                                      "data": [
                                        { "id": 12, "imageUrl": "https://example.com/12.jpg", "isPrimary": false, "displayOrder": 0 },
                                        { "id": 10, "imageUrl": "https://example.com/10.jpg", "isPrimary": true, "displayOrder": 1 },
                                        { "id": 11, "imageUrl": "https://example.com/11.jpg", "isPrimary": false, "displayOrder": 2 }
                                      ]
                                    }
                                    """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid, duplicate, or incomplete image IDs", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Field not found", content = @Content)
    })
    @PreAuthorize("hasRole('OWNER')")
    @PutMapping("/order")
    public ApiResponse<List<FieldImageDto>> updateImageOrder(
            @PathVariable UUID fieldId,
            @Valid @RequestBody FieldImageOrderRequest request) {
        return ApiResponse.success(
                "Image order updated successfully",
                fieldService.updateImageOrder(fieldId, request));
    }

    @Operation(
            summary = "Delete a field image",
            description = "Removes the image record from the database and deletes the file from Cloudinary."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Image deleted"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Image not found", content = @Content)
    })
    @PreAuthorize("hasRole('OWNER')")
    @DeleteMapping("/{imageId}")
    public ApiResponse<Void> deleteImage(
            @Parameter(description = "ID of the field") @PathVariable UUID fieldId,
            @Parameter(description = "ID of the image to delete") @PathVariable Long imageId) {
        fieldService.deleteImage(imageId);
        return ApiResponse.success("Image deleted successfully", null);
    }
}
