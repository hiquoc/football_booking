package com.project.user.service.impl;

import com.project.common.enums.UserType;
import com.project.common.cache.CacheKeys;
import com.project.common.cache.CacheNames;
import com.project.common.dto.PageResponse;
import com.project.common.exception.ForbiddenException;
import com.project.common.exception.NotFoundException;
import com.project.common.security.UserPrincipal;
import com.project.user.dto.UpdateProfileRequest;
import com.project.user.dto.UserDto;
import com.project.user.dto.PublicProfileDto;
import com.project.user.entity.User;
import com.project.user.kafka.UserProfileEventPublisher;
import com.project.user.kafka.UserNotificationEventPublisher;
import com.project.user.mapper.UserMapper;
import com.project.user.repository.UserRepository;
import com.project.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private static final String ACTIVE_STATUS = "ACTIVE";
    private static final String PLATFORM_BANNED_STATUS = "PLATFORM_BANNED";

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final UserProfileEventPublisher userProfileEventPublisher;
    private final UserNotificationEventPublisher userNotificationEventPublisher;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UserDto> getUsers(Pageable pageable) {
        return PageResponse.from(userRepository.findAll(pageable).map(userMapper::toDtoWithBan));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UserDto> getUsers(String phoneNumber, Pageable pageable) {
        String value = phoneNumber == null ? "" : phoneNumber.trim();
        if (value.isBlank()) {
            return getUsers(pageable);
        }
        return PageResponse.from(userRepository.findByPhoneNumberContainingIgnoreCase(value, pageable)
                .map(userMapper::toDtoWithBan));
    }

    @Override
    @Transactional(readOnly = true)
    public UserDto getEmployeeByPhone(String phoneNumber) {
        return getAssignableUserByPhone(phoneNumber);
    }

    @Override
    @Transactional(readOnly = true)
    public UserDto getAssignableUserByPhone(String phoneNumber) {
        String value = phoneNumber == null ? "" : phoneNumber.trim();
        User user = userRepository.findByPhoneNumber(value)
                .orElseThrow(() -> new NotFoundException("User not found with phone number: " + value));
        return userMapper.toDtoWithBan(user);
    }

    @Override
    @Cacheable(cacheNames = CacheNames.USER_BY_ID, key = CacheKeys.USER, sync = true)
    public UserDto getUserById(UUID id) {
        User user = getUser(id);
        return userMapper.toDtoWithBan(user);
    }

    @Override
    @Cacheable(cacheNames = CacheNames.USER_BY_ID, key = CacheKeys.USER_WITH_REQUESTER, sync = true)
    public UserDto getUserById(UUID id, UserPrincipal requester) {
        if (id.equals(requester.id())) {
            User user = getUser(id);
            return userMapper.toDtoWithBan(user);
        }
        User user = getUser(id);
        if (UserType.OWNER.name().equals(requester.role()) && user.getUserType() == UserType.EMPLOYEE) {
            return userMapper.toDtoWithBan(user);
        }
        if (!UserType.ADMIN.name().equals(requester.role())) {
            throw new ForbiddenException("You don't have permission to perform this operation");
        }
        return userMapper.toDtoWithBan(user);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CacheNames.USER_BY_ID, key = "'profile:' + #id", sync = true)
    public PublicProfileDto getPublicProfile(UUID id) {
        return userMapper.toPublicProfileDto(getUser(id));
    }

    @Override
    public UserDto getUserByPhone(String phone) {
        User user = userRepository.findByPhoneNumber(phone)
                .orElseThrow(() -> new NotFoundException("User not found with phone number: " + phone));
        return userMapper.toDtoWithBan(user);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = CacheNames.USER_BY_ID, allEntries = true)
    public UserDto updateUserProfile(UserPrincipal principal, UpdateProfileRequest request) {
        User user = userRepository.findById(principal.id())
                .orElseThrow(() -> new NotFoundException("User not found with id: " + principal.id()));

        if (request.getFullName() != null && !request.getFullName().isBlank()) {
            user.setFullName(request.getFullName());
        }
        if (request.getPhoneNumber() != null && !request.getPhoneNumber().isBlank()) {
            user.setPhoneNumber(request.getPhoneNumber());
        }
        if (request.getBio() != null) {
            user.setBio(request.getBio().isBlank() ? null : request.getBio());
        }
        if (request.getSkillLevel() != null) {
            user.setSkillLevel(request.getSkillLevel());
        }
        User saved = userRepository.save(user);
        userProfileEventPublisher.publishUpdated(saved);
        return userMapper.toDtoWithBan(saved);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = CacheNames.USER_BY_ID, allEntries = true)
    public UserDto changeUserRole(UUID actorId, UUID targetUserId, UserType newRole) {
        if (targetUserId.equals(actorId)) {
            throw new ForbiddenException("Admins cannot change their own role");
        }
        return changeUserRole(targetUserId, newRole);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = CacheNames.USER_BY_ID, allEntries = true)
    public UserDto changeUserRole(UUID targetUserId, UserType newRole) {
        if (newRole == UserType.ADMIN) {
            throw new ForbiddenException("Users cannot be upgraded to admin role");
        }
        User user = userRepository.findById(targetUserId)
                .orElseThrow(() -> new NotFoundException("User not found with id: " + targetUserId));
        user.setUserType(newRole);
        User saved = userRepository.save(user);
        userProfileEventPublisher.publishUpdated(saved);
        return userMapper.toDtoWithBan(saved);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = CacheNames.USER_BY_ID, allEntries = true)
    public UserDto changeUserStatus(UUID actorId, UUID targetUserId, String status) {
        if (targetUserId.equals(actorId)) {
            throw new ForbiddenException("Admins cannot change their own ban status");
        }
        String normalized = status == null ? "" : status.trim().toUpperCase();
        if (!ACTIVE_STATUS.equals(normalized) && !PLATFORM_BANNED_STATUS.equals(normalized)) {
            throw new ForbiddenException("Unsupported user status");
        }
        User user = userRepository.findById(targetUserId)
                .orElseThrow(() -> new NotFoundException("User not found with id: " + targetUserId));
        String previousStatus = user.getStatus();
        user.setStatus(normalized);
        User saved = userRepository.save(user);
        userProfileEventPublisher.publishUpdated(saved);
        publishStatusChangeNotification(actorId, saved, previousStatus, normalized);
        return userMapper.toDtoWithBan(saved);
    }

    private void publishStatusChangeNotification(UUID actorId, User user, String previousStatus, String nextStatus) {
        if (nextStatus.equals(previousStatus)) {
            return;
        }
        if (PLATFORM_BANNED_STATUS.equals(nextStatus)) {
            userNotificationEventPublisher.publishModerationNotification(
                    user.getId(),
                    "PLATFORM_BAN",
                    "Tài khoản của bạn đã bị cấm",
                    Map.of("updatedBy", actorId, "status", nextStatus));
            return;
        }
        if (PLATFORM_BANNED_STATUS.equals(previousStatus) && ACTIVE_STATUS.equals(nextStatus)) {
            userNotificationEventPublisher.publishModerationNotification(
                    user.getId(),
                    "PLATFORM_UNBAN",
                    "Tài khoản của bạn đã được gỡ cấm",
                    Map.of("updatedBy", actorId, "status", nextStatus));
        }
    }


    ////
    private User getUser(UUID id){
        return userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found with id: " + id));
    }
}
