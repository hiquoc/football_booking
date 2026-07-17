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

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UserDto> getUsers(Pageable pageable) {
        return PageResponse.from(userRepository.findAll(pageable).map(userMapper::toDto));
    }

    @Override
    @Cacheable(cacheNames = CacheNames.USER_BY_ID, key = CacheKeys.USER, sync = true)
    public UserDto getUserById(UUID id) {
        User user = getUser(id);
        return userMapper.toDto(user);
    }

    @Override
    @Cacheable(cacheNames = CacheNames.USER_BY_ID, key = CacheKeys.USER_WITH_REQUESTER, sync = true)
    public UserDto getUserById(UUID id, UserPrincipal requester) {
        if (id.equals(requester.id())) {
            User user = getUser(id);
            return userMapper.toDto(user);
        }
        if (!UserType.ADMIN.name().equals(requester.role())) {
            throw new ForbiddenException("You don't have permission to perform this operation");
        }
        return userMapper.toDto(getUser(id));
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
        return userMapper.toDto(user);
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
        return userMapper.toDto(userRepository.save(user));
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = CacheNames.USER_BY_ID, allEntries = true)
    public UserDto changeUserRole(UUID targetUserId, UserType newRole) {
        User user = userRepository.findById(targetUserId)
                .orElseThrow(() -> new NotFoundException("User not found with id: " + targetUserId));
        user.setUserType(newRole);
        return userMapper.toDto(userRepository.save(user));
    }


    ////
    private User getUser(UUID id){
        return userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found with id: " + id));
    }
}
