package com.project.user.service.impl;

import com.project.common.enums.UserType;
import com.project.common.exception.ForbiddenException;
import com.project.common.exception.NotFoundException;
import com.project.common.security.UserPrincipal;
import com.project.user.dto.UpdateProfileRequest;
import com.project.user.dto.UserDto;
import com.project.user.entity.User;
import com.project.user.mapper.UserMapper;
import com.project.user.repository.UserRepository;
import com.project.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Override
    public UserDto getUserById(UUID id) {
        User user = getUser(id);
        return userMapper.toDto(user);
    }

    @Override
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
    public UserDto getUserByPhone(String phone) {
        User user = userRepository.findByPhoneNumber(phone)
                .orElseThrow(() -> new NotFoundException("User not found with phone number: " + phone));
        return userMapper.toDto(user);
    }

    @Override
    @Transactional
    public UserDto updateUserProfile(UserPrincipal principal, UpdateProfileRequest request) {
        User user = userRepository.findById(principal.id())
                .orElseThrow(() -> new NotFoundException("User not found with id: " + principal.id()));

        if (request.getFullName() != null && !request.getFullName().isBlank()) {
            user.setFullName(request.getFullName());
        }
        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(request.getAvatarUrl());
        }

        return userMapper.toDto(userRepository.save(user));
    }

    @Override
    @Transactional
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
