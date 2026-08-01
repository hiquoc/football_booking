package com.project.user.service.impl;

import com.project.common.enums.UserType;
import com.project.common.exception.ForbiddenException;
import com.project.user.dto.UserDto;
import com.project.user.entity.User;
import com.project.user.kafka.UserProfileEventPublisher;
import com.project.user.mapper.UserMapper;
import com.project.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private UserProfileEventPublisher userProfileEventPublisher;

    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        userService = new UserServiceImpl(userRepository, userMapper, userProfileEventPublisher);
    }

    @Test
    void changeUserRole_shouldRejectAdminRole() {
        UUID targetUserId = UUID.randomUUID();

        assertThrows(ForbiddenException.class, () -> userService.changeUserRole(targetUserId, UserType.ADMIN));

        verify(userRepository, never()).findById(any());
        verify(userRepository, never()).save(any());
        verify(userProfileEventPublisher, never()).publishUpdated(any());
    }

    @Test
    void changeUserRole_shouldAllowNonAdminRole() {
        UUID targetUserId = UUID.randomUUID();
        User user = User.builder()
                .id(targetUserId)
                .fullName("Test User")
                .phoneNumber("0912345678")
                .userType(UserType.CLIENT)
                .status("ACTIVE")
                .build();
        UserDto dto = UserDto.builder()
                .id(targetUserId)
                .userType(UserType.OWNER)
                .build();

        when(userRepository.findById(targetUserId)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toDtoWithBan(user)).thenReturn(dto);

        UserDto result = userService.changeUserRole(targetUserId, UserType.OWNER);

        assertEquals(UserType.OWNER, user.getUserType());
        assertEquals(UserType.OWNER, result.getUserType());
        verify(userProfileEventPublisher).publishUpdated(user);
    }
}
