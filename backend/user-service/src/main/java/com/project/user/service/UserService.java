package com.project.user.service;

import com.project.common.enums.UserType;
import com.project.common.security.UserPrincipal;
import com.project.user.dto.UpdateProfileRequest;
import com.project.user.dto.UserDto;
import com.project.common.dto.PageResponse;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface UserService {
    PageResponse<UserDto> getUsers(Pageable pageable);
    UserDto getUserById(UUID id);
    UserDto getUserById(UUID id, UserPrincipal requester);
    UserDto getUserByPhone(String phone);
    UserDto updateUserProfile(UserPrincipal user, UpdateProfileRequest request);
    UserDto changeUserRole(UUID targetUserId, UserType newRole);
}
