package com.project.user.controller;

import com.project.common.dto.ApiResponse;
import com.project.common.dto.PageResponse;
import com.project.common.enums.UserType;
import com.project.common.security.CurrentUser;
import com.project.common.security.UserPrincipal;
import com.project.user.dto.ChangeRoleRequest;
import com.project.user.dto.UpdateProfileRequest;
import com.project.user.dto.UserDto;
import com.project.user.service.UserService;
import com.project.user.service.AvatarUploadService;
import com.project.user.dto.AvatarUploadSlotRequest;
import com.project.user.dto.AvatarUploadSlotDto;
import com.project.user.dto.AvatarUploadConfirmRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Pageable;
import org.springdoc.core.converters.models.PageableAsQueryParam;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "User profile management")
public class UserController {

    private final UserService userService;
    private final AvatarUploadService avatarUploadService;

    private static final String USER_EXAMPLE = """
            {
              "id": 42,
              "phoneNumber": "+84912345678",
              "email": "nguyenvana@example.com",
              "fullName": "Nguyen Van A",
              "avatarUrl": "https://cdn.example.com/avatars/42.png",
              "userType": "CLIENT",
              "socialProvider": null,
              "socialProviderId": null,
              "status": "ACTIVE",
              "createdAt": "2025-01-15T08:30:00",
              "updatedAt": "2025-06-01T12:00:00"
            }
            """;

    @Operation(summary = "Get users (Admin only)", description = "Returns a paginated list of users.")
    @PreAuthorize("hasRole('ADMIN')")
    @PageableAsQueryParam
    @GetMapping
    public ApiResponse<PageResponse<UserDto>> getUsers(Pageable pageable) {
        return ApiResponse.success(userService.getUsers(pageable));
    }

    @Operation(summary = "Get my profile", description = "Returns the profile of the currently authenticated user")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Profile retrieved",
                    content = @Content(schema = @Schema(implementation = UserDto.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "success": true,
                                      "message": "Profile retrieved successfully",
                                      "data": """ + USER_EXAMPLE + """
                                            }
                                            """)))
    })
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/me")
    public ApiResponse<UserDto> getMyProfile(@CurrentUser UserPrincipal user) {
        return ApiResponse.success("Profile retrieved successfully", userService.getUserById(user.id()));
    }

    @Operation(summary = "Get user by ID", description = "Returns the profile of any user by their ID")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Profile retrieved",
                    content = @Content(schema = @Schema(implementation = UserDto.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "success": true,
                                      "message": "Profile retrieved successfully",
                                      "data": """ + USER_EXAMPLE + """
                                            }
                                            """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User not found",
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "success": false,
                              "message": "User not found",
                              "data": null
                            }
                            """)))
    })
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{id}")
    public ApiResponse<UserDto> getUserProfile(@PathVariable UUID id, @CurrentUser UserPrincipal requester) {
        return ApiResponse.success("Profile retrieved successfully", userService.getUserById(id, requester));
    }

    @Operation(summary = "Update my profile",
            description = "Updates the authenticated user's full name. Avatars must use the dedicated signed upload endpoints.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true,
                    content = @Content(schema = @Schema(implementation = UpdateProfileRequest.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "fullName": "Nguyen Van B"
                                    }
                                    """))))
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Profile updated",
                    content = @Content(schema = @Schema(implementation = UserDto.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "success": true,
                                      "message": "Profile updated successfully",
                                      "data": """ + USER_EXAMPLE + """
                                            }
                                            """)))
    })
    @PreAuthorize("isAuthenticated()")
    @PatchMapping("/me")
    public ApiResponse<UserDto> updateMyProfile(
            @CurrentUser UserPrincipal user,
            @Valid @RequestBody UpdateProfileRequest request) {
        return ApiResponse.success("Profile updated successfully", userService.updateUserProfile(user, request));
    }

    @Operation(summary = "Request a signed Cloudinary avatar upload slot")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/me/avatar/upload-slot")
    public ApiResponse<AvatarUploadSlotDto> requestAvatarUpload(
            @CurrentUser UserPrincipal user, @Valid @RequestBody AvatarUploadSlotRequest request) {
        return ApiResponse.success("Avatar upload slot issued", avatarUploadService.issueSlot(user.id(), request));
    }

    @Operation(summary = "Confirm a direct Cloudinary avatar upload")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/me/avatar/confirm")
    public ApiResponse<UserDto> confirmAvatarUpload(
            @CurrentUser UserPrincipal user, @Valid @RequestBody AvatarUploadConfirmRequest request) {
        return ApiResponse.success("Avatar updated successfully", avatarUploadService.confirm(user.id(), request));
    }

    @Operation(summary = "Change user role (Admin only)",
            description = "Allows an ADMIN to change the role of any user. Accessible only to users with ADMIN role.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true,
                    content = @Content(schema = @Schema(implementation = ChangeRoleRequest.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "userType": "OWNER"
                                    }
                                    """))))
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Role changed successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden – admin role required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User not found")
    })
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}/role")
    public ApiResponse<UserDto> changeUserRole(
            @PathVariable UUID id,
            @Valid @RequestBody ChangeRoleRequest request) {
        return ApiResponse.success("Role updated successfully", userService.changeUserRole(id, request.getUserType()));
    }
}
