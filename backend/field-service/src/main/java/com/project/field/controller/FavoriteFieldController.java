package com.project.field.controller;

import com.project.common.dto.ApiResponse;
import com.project.common.security.CurrentUser;
import com.project.common.security.UserPrincipal;
import com.project.field.dto.FavoriteCheckResponse;
import com.project.field.dto.FieldDto;
import com.project.field.service.FavoriteFieldService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users/me/favorites")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CLIENT')")
public class FavoriteFieldController {

    private final FavoriteFieldService favoriteFieldService;

    @GetMapping
    public ApiResponse<List<FieldDto>> getFavorites(@CurrentUser UserPrincipal currentUser) {
        return ApiResponse.success(favoriteFieldService.getFavorites(currentUser.id()));
    }

    @PostMapping("/{fieldId}")
    public ApiResponse<FieldDto> addFavorite(
            @CurrentUser UserPrincipal currentUser,
            @PathVariable UUID fieldId) {
        return ApiResponse.success("Field added to favorites",
                favoriteFieldService.addFavorite(currentUser.id(), fieldId));
    }

    @DeleteMapping("/{fieldId}")
    public ApiResponse<Void> removeFavorite(
            @CurrentUser UserPrincipal currentUser,
            @PathVariable UUID fieldId) {
        favoriteFieldService.removeFavorite(currentUser.id(), fieldId);
        return ApiResponse.success("Field removed from favorites", null);
    }

    @GetMapping("/check/{fieldId}")
    public ApiResponse<FavoriteCheckResponse> checkFavorite(
            @CurrentUser UserPrincipal currentUser,
            @PathVariable UUID fieldId) {
        return ApiResponse.success(favoriteFieldService.checkFavorite(currentUser.id(), fieldId));
    }
}
